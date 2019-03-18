package com.semantica.pocketknife.methodrecorder;

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

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * A MethodRecorder can be used to record method invocations. It is initialized
 * with a class on which method calls are to be recorded. The
 * {@link MethodRecorder} object then creates a proxy instance of this class.
 * The methods calls to be recorded then should be invoked on this proxy
 * (obtained by {@link #getProxy()}. After invoking a method on its proxy, the
 * corresponding method name, a {@link java.lang.reflect.Method} or a
 * {@link MethodCall} can be retrieved via one of its getMethod* methods.
 *
 * A typical use is to record method invocations to verify method calls:
 *
 * <pre>
 * <code>
 * final int once = 1;
 * assert myMock.getCalls().verifyAndRemoveCall(once,
				myMockRecorder.getMethodCall(storeMockRecorder.getProxy().myMethod(someArg)));
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
	private final Class<T> proxyClass;
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
	 */
	@SuppressWarnings("unchecked")
	public MethodRecorder(Class<T> recordedClass) {
		super();
		this.recordedClass = recordedClass;
		Enhancer enhancer = new Enhancer();
		enhancer.setUseCache(false);
		enhancer.setSuperclass(this.recordedClass);
		enhancer.setCallbackType(MethodInterceptor.class);
		this.proxyClass = enhancer.createClass();
		Enhancer.registerCallbacks(proxyClass, new Callback[] { (MethodInterceptor) this::intercept });
		this.proxy = OBJENESIS.newInstance(proxyClass);
	}

	/**
	 * Static factory provides an alternative way to instantiate a method recorder.
	 *
	 * @param recordedClass
	 * @return
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
	public Class<T> getRecordedClass() {
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

	/**
	 * Method interceptor that does not call back any method on the superclass but
	 * simply registers the method call and returns a default value for the return
	 * type.
	 *
	 * @param obj    This proxy
	 * @param method The invoked method
	 * @param args   The arguments with which the method was invoked
	 * @param proxy  Can be used for any callbacks to the original method on an
	 *               instance of the proxy's superclass
	 * @return Return value for the intercepted method
	 * @throws Throwable
	 */
	public Object intercept(Object obj, java.lang.reflect.Method method, Object[] args, MethodProxy proxy)
			throws Throwable {
		AmbiguousArgumentsUtil.checkForIdentifierAmbiguity(args, matchers);
		this.method = method;
		this.methodCall = new MethodCall<>(method, substituteWithMatchingArgs(args));
		this.captureNumber = 0;
		this.captureProcessedNumber = 0;
		if (!this.matchers.isEmpty()) {
			throw new IllegalStateException(
					"Matchers not empty after substituting args with matchers for constructing new MethodCall.");
		}
		Object defaultValue = DefaultValues.defaultValue(method.getReturnType());
		log.debug("Returning {} for method {} in interceptor.", defaultValue, method);
		return defaultValue;
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

	public <S> String getMethodName(Callable<S> callableMethodInvoker) {
		return getMethod(callableMethodInvoker).getName();
	}

	public String getMethodName(ThrowingRunnable runnableMethodInvoker) {
		return getMethod(runnableMethodInvoker).getName();
	}

	/**
	 * Retrieve the corresponding method name after invoking a method.
	 *
	 * @return
	 */
	public String getMethodName() {
		return method.getName();
	}

	public String getMethodName(Object dummy) {
		return method.getName();
	}

	public String getMethodName(boolean dummy) {
		return method.getName();
	}

	public String getMethodName(byte dummy) {
		return method.getName();
	}

	public String getMethodName(char dummy) {
		return method.getName();
	}

	public String getMethodName(double dummy) {
		return method.getName();
	}

	public String getMethodName(float dummy) {
		return method.getName();
	}

	public String getMethodName(int dummy) {
		return method.getName();
	}

	public String getMethodName(long dummy) {
		return method.getName();
	}

	public String getMethodName(short dummy) {
		return method.getName();
	}

	public <S> Method getMethod(Callable<S> callableMethodInvoker) {
		return getMethodCall(callableMethodInvoker).getMethod();
	}

	public Method getMethod(ThrowingRunnable runnableMethodInvoker) {
		return getMethodCall(runnableMethodInvoker).getMethod();
	}

	/**
	 * Retrieve the corresponding {@link java.lang.reflect.Method} after invoking a
	 * method.
	 *
	 * @return
	 */
	public Method getMethod() {
		return method;
	}

	public Method getMethod(Object dummy) {
		return method;
	}

	public Method getMethod(boolean dummy) {
		return method;
	}

	public Method getMethod(byte dummy) {
		return method;
	}

	public Method getMethod(char dummy) {
		return method;
	}

	public Method getMethod(double dummy) {
		return method;
	}

	public Method getMethod(float dummy) {
		return method;
	}

	public Method getMethod(int dummy) {
		return method;
	}

	public Method getMethod(long dummy) {
		return method;
	}

	public Method getMethod(short dummy) {
		return method;
	}

	public <S> MethodCall<Method> getMethodCall(Callable<S> callableMethodInvoker) {
		try {
			callableMethodInvoker.call();
		} catch (Exception e) {
			log.debug("Exception was thrown while executing runnable method invoker.", e);
			throw new FatalTestException(e);
		}
		return methodCall;
	}

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
	 * Retrieve the corresponding {@link MethodCall} after invoking a method.
	 *
	 * @return
	 */
	public MethodCall<Method> getMethodCall() {
		return methodCall;
	}

	public MethodCall<Method> getMethodCall(Object dummy) {
		return methodCall;
	}

	public MethodCall<Method> getMethodCall(boolean dummy) {
		return methodCall;
	}

	public MethodCall<Method> getMethodCall(byte dummy) {
		return methodCall;
	}

	public MethodCall<Method> getMethodCall(char dummy) {
		return methodCall;
	}

	public MethodCall<Method> getMethodCall(double dummy) {
		return methodCall;
	}

	public MethodCall<Method> getMethodCall(float dummy) {
		return methodCall;
	}

	public MethodCall<Method> getMethodCall(int dummy) {
		return methodCall;
	}

	public MethodCall<Method> getMethodCall(long dummy) {
		return methodCall;
	}

	public MethodCall<Method> getMethodCall(short dummy) {
		return methodCall;
	}

	/**
	 * Use this method to wrap predicate matchers in a method call.
	 *
	 * @param predicate
	 * @param clazz
	 * @return
	 */
	public <S> S storeAndCreateIdInstanceOfTypeArgument(Predicate<S> predicate, Class<S> clazz) {
		return storeAndCreateIdInstanceOfTypeArgument(predicate, clazz, Optional.empty());
	}

	/**
	 * Use this method to wrap Matchers in a method call.
	 *
	 * Typical use:
	 *
	 * <pre>
	 * <code>
	 * {@code Matcher<Integer>} matcher = Matchers.any(int.class); // ! Hamcrest matchers do not implement equals(Object obj)
	 * assert methodRecorder
	 *    .getMethodCall(methodRecorder.getProxy()
	 *       .oneParameter((randomIntermediateIdentifier = methodRecorder
	 *          .storeAndCreateIdInstanceOfTypeArgument(matcher, int.class))))
	 *    .equals(new {@code MethodCall<>}(Methods.class.getMethod("oneParameter", int.class), matcher));
	 * </code>
	 * </pre>
	 *
	 * @param matcher
	 * @param clazz
	 * @return
	 */
	public <S> S storeAndCreateIdInstanceOfTypeArgument(Matcher<S> matcher, Class<S> clazz) {
		return storeAndCreateIdInstanceOfTypeArgument(matcher, clazz, Optional.empty());
	}

	/**
	 * Use this method to prevent ambiguity and ensure the Predicate is linked to
	 * the correct parameter.
	 *
	 * @param predicate
	 * @param clazz
	 * @param argumentNumber
	 * @return
	 */
	public <S> S storeAndCreateIdInstanceOfTypeArgument(Predicate<S> predicate, Class<S> clazz, int argumentNumber) {
		return storeAndCreateIdInstanceOfTypeArgument(predicate, clazz, Optional.of(argumentNumber));
	}

	/**
	 * Use this method to prevent ambiguity and ensure the Matcher is linked to the
	 * correct parameter.
	 *
	 * @param matcher
	 * @param clazz
	 * @param argumentNumber
	 * @return
	 */
	public <S> S storeAndCreateIdInstanceOfTypeArgument(Matcher<S> matcher, Class<S> clazz, int argumentNumber) {
		return storeAndCreateIdInstanceOfTypeArgument(matcher, clazz, Optional.of(argumentNumber));
	}

	private <S> S storeAndCreateIdInstanceOfTypeArgument(Object matcher, Class<S> clazz,
			Optional<Integer> argumentNumber) {
		S identifierValue = RandomValues.identifierValue(clazz);
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

	static class FatalTestException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public FatalTestException(Throwable cause) {
			super(cause);
		}
	}

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
