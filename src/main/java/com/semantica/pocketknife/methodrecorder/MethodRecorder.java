package com.semantica.pocketknife.methodrecorder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import org.hamcrest.Matcher;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import com.semantica.pocketknife.calls.MethodCall;
import com.semantica.pocketknife.methodrecorder.AmbiguousArgumentsUtil.AmbiguouslyDefinedMatchersException;
import com.semantica.pocketknife.methodrecorder.dynamicproxies.ClassLoadingStrategyFinder;
import com.semantica.pocketknife.methodrecorder.dynamicproxies.Dummy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * A MethodRecorder can be used to record method invocations. It is initialized
 * with a class on which method calls are to be recorded. The
 * {@link MethodRecorder} object creates a proxy instance of this class upon
 * construction. The methods calls to be recorded should be invoked on this
 * proxy (obtained by {@link #getProxy()}. After invoking a method on its proxy,
 * the corresponding method name, a {@link java.lang.reflect.Method} or a
 * {@link MethodCall} can be retrieved via one of its getMethod* methods.
 *
 * A typical use is to record method invocations when verifying method calls in
 * a test:
 *
 * <pre>
 * <code>
 * assert myMock.getCalls().verifyAndRemoveCall(
 *    Invoked.ONCE, myMockRecorder.getMethodCall(
 *       myMockRecorder.getProxy().someMethod(someArg)));
 * </code>
 * </pre>
 *
 * @author A. Haanstra
 *
 * @param <T>
 */
public class MethodRecorder<T> {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MethodRecorder.class);
	private static final Objenesis OBJENESIS = new ObjenesisStd();
	private final Class<T> recordedClass;
	private final Class<? extends T> proxyClass;
	private final T proxy;
	private final Map<Class<?>, Map<Object, Queue<MatchingArgument>>> matchers = new HashMap<>();
	private Method method;
	private MethodCall<Method> methodCall;
	private int captureNumber = 0;
	private int captureProcessedNumber = 0;

	/**
	 * Constructs a MethodRecorder instance that can record method invocations for
	 * the {@code recordedClass} on its proxy instance.
	 *
	 * @param recordedClass
	 * @throws IllegalAccessException
	 */
	@SuppressWarnings("unchecked")
	public MethodRecorder(Class<T> recordedClass) {
		super();
		this.recordedClass = recordedClass;
		ClassLoadingStrategyFinder<Dummy> strategyFinder = new ClassLoadingStrategyFinder<>(Dummy.class);
		ClassLoadingStrategy<ClassLoader> strategy = strategyFinder
				.getClassLoadingStrategyToDefineClassInSamePackageAsClassInTargetPackage();
//		this.proxyClass = new ByteBuddy().subclass(recordedClass)
//				.name(strategyFinder.getTargetClassNameUniqueForTargetClassMatchingStrategy(recordedClass,
//						"MethodRecorderProxy"))
//				.method(ElementMatchers.any()).intercept(MethodDelegation.to(new Interceptor())).make()
//				.load(strategyFinder.getClassLoader(), strategy).getLoaded();

		this.proxyClass = new ByteBuddy().subclass(recordedClass)
				.name(strategyFinder.getTargetClassNameUniqueForTargetClassMatchingStrategy(recordedClass,
						"MethodRecorderProxy"))
				.method(ElementMatchers.any()).intercept(InvocationHandlerAdapter.of(new CallHandler())).make()
				.load(strategyFinder.getClassLoader(), strategy).getLoaded();

		this.proxy = OBJENESIS.newInstance(proxyClass);
	}

	/**
	 * Static factory provides an alternative way to instantiate a method recorder.
	 *
	 * @param recordedClass
	 * @return
	 * @throws IllegalAccessException
	 */
	public static <T> MethodRecorder<T> recordInvocationsOn(Class<T> recordedClass) {
		return new MethodRecorder<>(recordedClass);
	}

	/**
	 * Gets the proxy superclass of the proxy instance on which method invocations
	 * are recorded.
	 *
	 * @return The proxy superclass
	 */
	public Class<? extends T> getRecordedClass() {
		return recordedClass;
	}

	/**
	 * Gets the proxy instance on which methods should be invoked to be recorded.
	 *
	 * @return The proxy
	 */
	public T getProxy() {
		return proxy;
	}

	private class CallHandler implements InvocationHandler {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			System.out.println("In method recorder intercept method!");
			if (method.getName().equals("toString") && args.length == 0) {
				return "Proxy recording method invocations on: " + recordedClass + ", identity hashCode: "
						+ System.identityHashCode(proxy);
			} else if (method.getName().equals("hashCode") && args.length == 0) {
				return System.identityHashCode(proxy);
			} else if (method.getName().equals("equals") && args.length == 1
					&& method.getParameterTypes()[0].equals(Object.class)) {
				return System.identityHashCode(proxy) == System.identityHashCode(args[0]);
			}
			AmbiguousArgumentsUtil.checkForIdentifierAmbiguity(args, matchers);
			MethodRecorder.this.method = method;
			MethodRecorder.this.methodCall = new MethodCall<>(method, substituteWithMatchingArgs(args));
			MethodRecorder.this.captureNumber = 0;
			MethodRecorder.this.captureProcessedNumber = 0;
			if (!MethodRecorder.this.matchers.isEmpty()) {
				throw new IllegalStateException(
						"Matchers not empty after substituting args with matchers for constructing new MethodCall.");
			}
			Object defaultValue = DefaultValues.defaultValue(method.getReturnType());
			log.trace("Returning {} for method {} in interceptor.", defaultValue, method);
			return defaultValue;
		}

	}

	public class Interceptor {
		/**
		 * Method interceptor that does not call back any method on the superclass but
		 * simply registers the method call and returns a default value for the return
		 * type.
		 *
		 * @param method The invoked method
		 * @param self   This proxy
		 * @param args   The arguments with which the method was invoked
		 * @param zuper  Can be used for any callbacks to the original method on an
		 *               instance of the proxy's superclass
		 * @return Return value for the intercepted method
		 * @throws Throwable
		 */
		@RuntimeType
		public Object intercept(@Origin Method method, @This Object self, @AllArguments Object[] args,
				@SuperCall Callable<String> zuper) throws Exception {
			System.out.println("In method recorder intercept method!");
			if (method.getName().equals("toString") && args.length == 0) {
				return "Proxy recording method invocations on: " + recordedClass + ", identity hashCode: "
						+ System.identityHashCode(self);
			} else if (method.getName().equals("hashCode") && args.length == 0) {
				return System.identityHashCode(self);
			} else if (method.getName().equals("equals") && args.length == 1
					&& method.getParameterTypes()[0].equals(Object.class)) {
				return System.identityHashCode(self) == System.identityHashCode(args[0]);
			}
			AmbiguousArgumentsUtil.checkForIdentifierAmbiguity(args, matchers);
			MethodRecorder.this.method = method;
			MethodRecorder.this.methodCall = new MethodCall<>(method, substituteWithMatchingArgs(args));
			MethodRecorder.this.captureNumber = 0;
			MethodRecorder.this.captureProcessedNumber = 0;
			if (!MethodRecorder.this.matchers.isEmpty()) {
				throw new IllegalStateException(
						"Matchers not empty after substituting args with matchers for constructing new MethodCall.");
			}
			Object defaultValue = DefaultValues.defaultValue(method.getReturnType());
			log.trace("Returning {} for method {} in interceptor.", defaultValue, method);
			return defaultValue;
		}
	}

	/**
	 * Resets all internal state. For typical use (recording multiple methods one
	 * after the other), it is not necessary to invoke this method in between
	 * recordings.
	 */
	public void reset() {
		this.method = null;
		this.methodCall = null;
		this.matchers.clear();
		this.captureNumber = 0;
		this.captureProcessedNumber = 0;
	}

	private Object[] substituteWithMatchingArgs(Object[] args) {
		for (int i = 0; i < args.length; i++) {
			args[i] = getOptionalMatchingValue(args[i], i).orElse(args[i]);
		}
		return args;
	}

	private Optional<Object> getOptionalMatchingValue(Object argument, int argumentNumber) {
		if (argument != null) {
			Map<Object, Queue<MatchingArgument>> matchersForClass = matchers.get(argument.getClass());
			if (matchersForClass != null) {
				Queue<MatchingArgument> matchersForIdentifierValue = matchersForClass.get(argument);
				if (matchersForIdentifierValue != null) {
					MatchingArgument matcherCandidate = matchersForIdentifierValue.element();
					if (matcherCandidate.getCaptureNumber() == captureProcessedNumber
							&& (!matcherCandidate.getArgumentNumber().isPresent()
									|| matcherCandidate.getArgumentNumber().get() == argumentNumber)) {
						captureProcessedNumber++;
						Object matcher = matchersForIdentifierValue.remove().getMatcher();
						if (matchersForIdentifierValue.isEmpty()) {
							matchersForClass.remove(argument);
							if (matchersForClass.isEmpty()) {
								matchers.remove(argument.getClass());
							}
						}
						return Optional.of(matcher);
					}
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * Returns a method name instead of a {@link MethodCall}, otherwise the same as
	 * {@link #getMethodCall(Callable)}.
	 *
	 * @param callableMethodInvoker The callable that calls the method to be
	 *                              recorded
	 * @return The method name of the called method
	 */
	public <S> String getMethodName(Callable<S> callableMethodInvoker) {
		return getMethod(callableMethodInvoker).getName();
	}

	/**
	 * Returns a method name instead of a {@link MethodCall}, otherwise the same as
	 * {@link #getMethodCall(ThrowingRunnable)}.
	 *
	 * @param callableMethodInvoker The callable that calls the method to be
	 *                              recorded
	 * @return The method name of the called method
	 */
	public String getMethodName(ThrowingRunnable runnableMethodInvoker) {
		return getMethod(runnableMethodInvoker).getName();
	}

	/**
	 * Retrieve the corresponding method name after invoking a method. This method
	 * is not part of the fluent API.
	 *
	 * @return The name of the method recorded, corresponding to the method last
	 *         invoked on this {@link MethodRecorder}.
	 */
	public String getMethodName() {
		return method.getName();
	}

	/**
	 * Retrieve the name of the recorded method that returned an {@link Object}
	 * reference. Part of the fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@link Object} reference value
	 *              returned from the method call to this {@link MethodRecorder}'s
	 *              {@link #proxy}.
	 * @return The name of the method recorded, corresponding to invoked method.
	 */
	public String getMethodName(Object dummy) {
		return method.getName();
	}

	/**
	 * Retrieve the recorded method that returned a {@code boolean} value. Part of
	 * the fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code boolean} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The name of the method recorded, corresponding to invoked method.
	 */
	public String getMethodName(boolean dummy) {
		return method.getName();
	}

	/**
	 * Retrieve the recorded method that returned a {@code byte} value. Part of the
	 * fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code byte} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The name of the method recorded, corresponding to invoked method.
	 */
	public String getMethodName(byte dummy) {
		return method.getName();
	}

	/**
	 * Retrieve the recorded method that returned a {@code char} value. Part of the
	 * fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code char} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The name of the method recorded, corresponding to invoked method.
	 */
	public String getMethodName(char dummy) {
		return method.getName();
	}

	/**
	 * Retrieve the recorded method that returned a {@code double} value. Part of
	 * the fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code double} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The name of the method recorded, corresponding to invoked method.
	 */
	public String getMethodName(double dummy) {
		return method.getName();
	}

	/**
	 * Retrieve the recorded method that returned a {@code float} value. Part of the
	 * fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code float} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The name of the method recorded, corresponding to invoked method.
	 */
	public String getMethodName(float dummy) {
		return method.getName();
	}

	/**
	 * Retrieve the recorded method that returned a {@code int} value. Part of the
	 * fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code int} value returned from the
	 *              method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The name of the method recorded, corresponding to invoked method.
	 */
	public String getMethodName(int dummy) {
		return method.getName();
	}

	/**
	 * Retrieve the recorded method that returned a {@code long} value. Part of the
	 * fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code long} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The name of the method recorded, corresponding to invoked method.
	 */
	public String getMethodName(long dummy) {
		return method.getName();
	}

	/**
	 * Retrieve the recorded method that returned a {@code short} value. Part of the
	 * fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code short} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The name of the method recorded, corresponding to invoked method.
	 */
	public String getMethodName(short dummy) {
		return method.getName();
	}

	/**
	 * Returns a {@link java.lang.reflect.Method} instead of a {@link MethodCall},
	 * otherwise the same as {@link #getMethodCall(Callable)}.
	 *
	 * @param callableMethodInvoker The callable that calls the method to be
	 *                              recorded
	 * @return A {@link java.lang.reflect.Method} that identifies the called method
	 */
	public <S> Method getMethod(Callable<S> callableMethodInvoker) {
		return getMethodCall(callableMethodInvoker).getMethod();
	}

	/**
	 * Returns a {@link java.lang.reflect.Method} instead of a {@link MethodCall},
	 * otherwise the same as {@link #getMethodCall(ThrowingRunnable)}.
	 *
	 * @param callableMethodInvoker The callable that calls the method to be
	 *                              recorded
	 * @return A {@link java.lang.reflect.Method} that identifies the called method
	 */
	public Method getMethod(ThrowingRunnable runnableMethodInvoker) {
		return getMethodCall(runnableMethodInvoker).getMethod();
	}

	/**
	 * Retrieve the corresponding {@link java.lang.reflect.Method} after invoking a
	 * method. This method is not part of the fluent API.
	 *
	 * @return The recorded {@link java.lang.reflect.Method} corresponding to the
	 *         method last invoked on this {@link MethodRecorder}.
	 */
	public Method getMethod() {
		return method;
	}

	/**
	 * Retrieve the recorded method that returned an {@link Object} reference. Part
	 * of the fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@link Object} reference value
	 *              returned from the method call to this {@link MethodRecorder}'s
	 *              {@link #proxy}.
	 * @return The recorded {@link java.lang.reflect.Method} corresponding to
	 *         invoked method.
	 */
	public Method getMethod(Object dummy) {
		return method;
	}

	/**
	 * Retrieve the recorded method that returned a {@code boolean} value. Part of
	 * the fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code boolean} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The recorded {@link java.lang.reflect.Method} corresponding to
	 *         invoked method.
	 */
	public Method getMethod(boolean dummy) {
		return method;
	}

	/**
	 * Retrieve the recorded method that returned a {@code byte} value. Part of the
	 * fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code byte} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The recorded {@link java.lang.reflect.Method} corresponding to
	 *         invoked method.
	 */
	public Method getMethod(byte dummy) {
		return method;
	}

	/**
	 * Retrieve the recorded method that returned a {@code char} value. Part of the
	 * fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code char} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The recorded {@link java.lang.reflect.Method} corresponding to
	 *         invoked method.
	 */
	public Method getMethod(char dummy) {
		return method;
	}

	/**
	 * Retrieve the recorded method that returned a {@code double} value. Part of
	 * the fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code double} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The recorded {@link java.lang.reflect.Method} corresponding to
	 *         invoked method.
	 */
	public Method getMethod(double dummy) {
		return method;
	}

	/**
	 * Retrieve the recorded method that returned a {@code float} value. Part of the
	 * fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code float} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The recorded {@link java.lang.reflect.Method} corresponding to
	 *         invoked method.
	 */
	public Method getMethod(float dummy) {
		return method;
	}

	/**
	 * Retrieve the recorded method that returned a {@code int} value. Part of the
	 * fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code int} value returned from the
	 *              method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The recorded {@link java.lang.reflect.Method} corresponding to
	 *         invoked method.
	 */
	public Method getMethod(int dummy) {
		return method;
	}

	/**
	 * Retrieve the recorded method that returned a {@code long} value. Part of the
	 * fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code long} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The recorded {@link java.lang.reflect.Method} corresponding to
	 *         invoked method.
	 */
	public Method getMethod(long dummy) {
		return method;
	}

	/**
	 * Retrieve the recorded method that returned a {@code short} value. Part of the
	 * fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code short} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The recorded {@link java.lang.reflect.Method} corresponding to
	 *         invoked method.
	 */
	public Method getMethod(short dummy) {
		return method;
	}

	/**
	 * While most getMethod*(..) methods just accept a dummy whose value is ignored,
	 * this method actually accepts a lambda that can execute a method with a return
	 * value. The lambda is called to execute the method wrapped by it and the
	 * recorded {@link MethodCall} is returned.
	 *
	 * This method is particularly for recording a simple method call (a return
	 * value but no parameters) since then a method reference can be used as
	 * argument.
	 *
	 * Typical use:
	 *
	 * <pre>
	 * <code>
	 * assert myMock.getCalls().verifyAndRemoveCall(
	 *    Invoked.ONCE, myMockRecorder.getMethodCall(
	 *       myMockRecorder.getProxy()::someMethod));
	 * </code>
	 * </pre>
	 *
	 * @param callableMethodInvoker The callable that calls the method to be
	 *                              recorded
	 * @return A {@link MethodCall} that identifies the called method
	 */
	public <S> MethodCall<Method> getMethodCall(Callable<S> callableMethodInvoker) {
		try {
			callableMethodInvoker.call();
		} catch (Exception e) {
			log.debug("Exception was thrown while executing runnable method invoker.", e);
			throw new FatalTestException(e);
		}
		return methodCall;
	}

	/**
	 * This method should be used when the method to be recorded is a void method.
	 * The other signatures cannot be used fluently since they all accept a dummy
	 * argument.
	 *
	 * Typical use:
	 *
	 * <pre>
	 * <code>
	 * assert myMock.getCalls().verifyAndRemoveCall(
	 *    Invoked.ONCE, myMockRecorder.getMethodCall(
	 *       ()->myMockRecorder.getProxy().someVoidMethod()));
	 * </code>
	 * </pre>
	 *
	 * @param runnableMethodInvoker The runnable that calls the (void) method to be
	 *                              recorded.
	 * @return A {@link MethodCall} that identifies the called method
	 */
	public MethodCall<Method> getMethodCall(ThrowingRunnable runnableMethodInvoker) {
		try {
			runnableMethodInvoker.run();
		} catch (Exception e) {
			log.debug("Exception was thrown while executing runnable method invoker.", e);
			throw new FatalTestException(e);
		}
		return methodCall;
	}

	/**
	 * Retrieve the corresponding {@link MethodCall} after invoking a method. This
	 * method is not part of the fluent API.
	 *
	 * @return The recorded {@link MethodCall} corresponding to the method last
	 *         invoked on this {@link MethodRecorder}.
	 */
	public MethodCall<Method> getMethodCall() {
		return methodCall;
	}

	/**
	 * Retrieve the recorded method call that returned an {@link Object} reference.
	 * Part of the fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@link Object} reference value
	 *              returned from the method call to this {@link MethodRecorder}'s
	 *              {@link #proxy}.
	 * @return The recorded {@link MethodCall} corresponding to invoked method.
	 */
	public MethodCall<Method> getMethodCall(Object dummy) {
		return methodCall;
	}

	/**
	 * Retrieve the recorded method call that returned a {@code boolean} value. Part
	 * of the fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code boolean} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The recorded {@link MethodCall} corresponding to invoked method.
	 */
	public MethodCall<Method> getMethodCall(boolean dummy) {
		return methodCall;
	}

	/**
	 * Retrieve the recorded method call that returned a {@code byte} value. Part of
	 * the fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code byte} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The recorded {@link MethodCall} corresponding to invoked method.
	 */
	public MethodCall<Method> getMethodCall(byte dummy) {
		return methodCall;
	}

	/**
	 * Retrieve the recorded method call that returned a {@code char} value. Part of
	 * the fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code char} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The recorded {@link MethodCall} corresponding to invoked method.
	 */
	public MethodCall<Method> getMethodCall(char dummy) {
		return methodCall;
	}

	/**
	 * Retrieve the recorded method call that returned a {@code double} value. Part
	 * of the fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code double} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The recorded {@link MethodCall} corresponding to invoked method.
	 */
	public MethodCall<Method> getMethodCall(double dummy) {
		return methodCall;
	}

	/**
	 * Retrieve the recorded method call that returned a {@code float} value. Part
	 * of the fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code float} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The recorded {@link MethodCall} corresponding to invoked method.
	 */
	public MethodCall<Method> getMethodCall(float dummy) {
		return methodCall;
	}

	/**
	 * Retrieve the recorded method call that returned a {@code int} value. Part of
	 * the fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code int} value returned from the
	 *              method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The recorded {@link MethodCall} corresponding to invoked method.
	 */
	public MethodCall<Method> getMethodCall(int dummy) {
		return methodCall;
	}

	/**
	 * Retrieve the recorded method call that returned a {@code long} value. Part of
	 * the fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code long} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The recorded {@link MethodCall} corresponding to invoked method.
	 */
	public MethodCall<Method> getMethodCall(long dummy) {
		return methodCall;
	}

	/**
	 * Retrieve the recorded method call that returned a {@code short} value. Part
	 * of the fluent API.
	 *
	 * @param dummy Dummy variable to accept the {@code short} value returned from
	 *              the method call to this {@link MethodRecorder}'s {@link #proxy}.
	 * @return The recorded {@link MethodCall} corresponding to invoked method.
	 */
	public MethodCall<Method> getMethodCall(short dummy) {
		return methodCall;
	}

	/**
	 * Use this method to wrap Matchers in a method call. This method returns an
	 * identifier value of the corresponding parameter type for internal retrieval
	 * of the matching argument (here {@link Predicate}).
	 *
	 * Example of a typical use with demonstration of the the place where the
	 * (random) identifier value is returned:
	 *
	 * <pre>
	 * <code>
	 * MethodRecorder myMethodRecorder = new MethodRecorder<>(MyMethods.class);
	 * {@code Matcher<Integer>} myMatcher = Matchers.any(int.class); // ! Hamcrest matchers do not implement equals(Object obj)
	 * int randomIntermediateIdentifier = 0;
	 * assert myMethodRecorder
	 *    .getMethodCall(myMethodRecorder.getProxy()
	 *       .someMethod((randomIntermediateIdentifier = myMethodRecorder
	 *          .storeAndCreateIdInstanceOfTypeArgument(myMatcher, int.class))))
	 *    .equals(new {@code MethodCall<>}(MyMethods.class.getMethod("someMethod", int.class), myMatcher));
	 * </code>
	 * </pre>
	 *
	 * @param predicate The matcher that should be used instead of an exact matching
	 *                  value as argument to the recorded method.
	 * @param clazz     The class of the type where the {@code predicate} is
	 *                  parameterized over (this information otherwise cannot be
	 *                  retrieved at runtime from the Matcher).
	 * @return An identifying value of the same type as the {@code predicate} is
	 *         parameterized over.
	 * @throws IllegalAccessException
	 */
	public <S> S storeAndCreateIdInstanceOfTypeArgument(Predicate<S> predicate, Class<S> clazz) {
		return storeMatcherAndCreateIdInstanceOfTypeArgumentAsKeyToMatcher(predicate, clazz, Optional.empty());
	}

	/**
	 * Use this method to wrap Matchers in a method call. This method returns an
	 * identifier value of the corresponding parameter type for internal retrieval
	 * of the matching argument (here {@link Matcher}).
	 *
	 * Example of a typical use with demonstration of the the place where the
	 * (random) identifier value is returned:
	 *
	 * <pre>
	 * <code>
	 * MethodRecorder myMethodRecorder = new MethodRecorder<>(MyMethods.class);
	 * {@code Matcher<Integer>} myMatcher = Matchers.any(int.class); // ! Hamcrest matchers do not implement equals(Object obj)
	 * int randomIntermediateIdentifier = 0;
	 * assert myMethodRecorder
	 *    .getMethodCall(myMethodRecorder.getProxy()
	 *       .someMethod((randomIntermediateIdentifier = myMethodRecorder
	 *          .storeAndCreateIdInstanceOfTypeArgument(myMatcher, int.class))))
	 *    .equals(new {@code MethodCall<>}(MyMethods.class.getMethod("someMethod", int.class), myMatcher));
	 * </code>
	 * </pre>
	 *
	 * @param matcher The matching argument that should be used instead of an exact
	 *                matching value as argument to the recorded method.
	 * @param clazz   The class of the type where the {@code matcher} is
	 *                parameterized over (this information otherwise cannot be
	 *                retrieved at runtime from the Matcher).
	 * @return An identifying value of the same type as the {@code matcher} is
	 *         parameterized over.
	 * @throws IllegalAccessException
	 */
	public <S> S storeAndCreateIdInstanceOfTypeArgument(Matcher<S> matcher, Class<S> clazz) {
		return storeMatcherAndCreateIdInstanceOfTypeArgumentAsKeyToMatcher(matcher, clazz, Optional.empty());
	}

	/**
	 * This method can be used to prevent any ambiguity around the used
	 * {@link Predicate}(s). Use this method to prevent ambiguity and ensure the
	 * {@link Predicate} is linked to the correct parameter. For tests of production
	 * code, it is *strongly* recommended either to use this method OR use only
	 * (wrapped) matchers in the method call. When using the other signatures, there
	 * is a small but real chance that otherwise passing tests fail randomly with an
	 * {@link AmbiguouslyDefinedMatchersException}. Use this method to ensure that
	 * tests are (R)epeatable (see F.I.R.S.T. Principles of Unit Testing).
	 *
	 * @param predicate      The matching argument that should be used instead of an
	 *                       exact matching value as argument to the recorded
	 *                       method.
	 * @param clazz          The class of the type where the {@code predicate} is
	 *                       parameterized over (this information otherwise cannot
	 *                       be retrieved at runtime from the {@code predicate}).
	 * @param argumentNumber The number of this matching argument in this method
	 *                       call counted from left to right and starting at 0.
	 * @return An identifying value of the same type as the {@code predicate} is
	 *         parameterized over.
	 * @throws IllegalAccessException
	 */
	public <S> S storeAndCreateIdInstanceOfTypeArgument(Predicate<S> predicate, Class<S> clazz, int argumentNumber) {
		return storeMatcherAndCreateIdInstanceOfTypeArgumentAsKeyToMatcher(predicate, clazz,
				Optional.of(argumentNumber));
	}

	/**
	 * This method can be used to prevent any ambiguity around the used
	 * {@link Matcher}(s). Use this method to prevent ambiguity and ensure the
	 * {@link Matcher} is linked to the correct parameter. For tests of production
	 * code, it is *strongly* recommended either to use this method OR use only
	 * (wrapped) matchers in the method call. When using the other signatures, there
	 * is a small but real chance that otherwise passing tests fail randomly with an
	 * {@link AmbiguouslyDefinedMatchersException}. Use this method to ensure that
	 * tests are (R)epeatable (see F.I.R.S.T. Principles of Unit Testing).
	 *
	 * @param matcher        The matching argument that should be used instead of an
	 *                       exact matching value as argument to the recorded
	 *                       method.
	 * @param clazz          The class of the type where the {@code matcher} is
	 *                       parameterized over (this information otherwise cannot
	 *                       be retrieved at runtime from the {@code matcher}).
	 * @param argumentNumber The number of this matching argument in this method
	 *                       call counted from left to right and starting at 0.
	 * @return An identifying value of the same type as the {@code matcher} is
	 *         parameterized over.
	 * @throws IllegalAccessException
	 */
	public <S> S storeAndCreateIdInstanceOfTypeArgument(Matcher<S> matcher, Class<S> clazz, int argumentNumber) {
		return storeMatcherAndCreateIdInstanceOfTypeArgumentAsKeyToMatcher(matcher, clazz, Optional.of(argumentNumber));
	}

	protected <S> S storeMatcherAndCreateIdInstanceOfTypeArgumentAsKeyToMatcher(Object matcher, Class<S> clazz,
			Optional<Integer> argumentNumber) {
		S identifierValue = RandomIdentifierValues.identifierValue(clazz);
		return storeMatcherWithIdInstanceOfTypeArgumentAsKey(matcher, clazz, argumentNumber, identifierValue);
	}

	protected <S> S storeMatcherWithIdInstanceOfTypeArgumentAsKey(Object matcher, Class<S> clazz,
			Optional<Integer> argumentNumber, S identifierValue) {
		Class<?> identifierClass = identifierValue.getClass();
		Map<Object, Queue<MatchingArgument>> matchersForClass = matchers.get(identifierClass);
		if (matchersForClass == null) {
			matchersForClass = new HashMap<>();
			matchers.put(identifierClass, matchersForClass);
		}
		Queue<MatchingArgument> matcherArgumentsForSameIdentifier = matchersForClass.get(identifierValue);
		if (matcherArgumentsForSameIdentifier == null) {
			matcherArgumentsForSameIdentifier = new ArrayDeque<>();
			matchersForClass.put(identifierValue, matcherArgumentsForSameIdentifier);
		}
		matcherArgumentsForSameIdentifier.add(new MatchingArgument(captureNumber++, matcher, argumentNumber));
		return identifierValue;
	}

	/**
	 * For unit-testing purposes.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + captureNumber;
		result = prime * result + captureProcessedNumber;
		result = prime * result + ((matchers == null) ? 0 : matchers.hashCode());
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result + ((methodCall == null) ? 0 : methodCall.hashCode());
		result = prime * result + ((proxy == null) ? 0 : proxy.hashCode());
		result = prime * result + ((proxyClass == null) ? 0 : proxyClass.hashCode());
		result = prime * result + ((recordedClass == null) ? 0 : recordedClass.hashCode());
		return result;
	}

}
