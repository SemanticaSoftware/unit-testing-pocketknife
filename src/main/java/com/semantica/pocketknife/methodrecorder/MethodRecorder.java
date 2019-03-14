package com.semantica.pocketknife.methodrecorder;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
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

public class MethodRecorder<T> {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MethodRecorder.class);
	private static final Objenesis OBJENESIS = new ObjenesisStd();
	private final Class<T> recordedClass;
	private final Class<T> proxyClass;
	private final T proxy;
	private final Map<Class<?>, Map<Object, Queue<MatchingArgument>>> matchers = new HashMap<>();
	private final Set<Object> identifierValues = new HashSet<>();
	private Method method;
	private MethodCall<Method> methodCall;
	private int captureNumber = 0;
	private int captureProcessedNumber = 0;

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
		AmbiguousArgumentsUtil.checkForIdentifierAmbiguity(args, identifierValues, matchers);
		this.identifierValues.clear();
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

	public void reset() {
		this.method = null;
		this.methodCall = null;
		this.matchers.clear();
		this.identifierValues.clear();
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
			debugAid1(argument);
			if (matchersForClass != null) {
				Queue<MatchingArgument> matchersForIdentifierValue = matchersForClass.get(argument);
				debugAid2(argument, matchersForClass);
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

	// TODO: Remove debug aid
	private void debugAid1(Object argument) {
		Class<?> argumentClass = argument.getClass();
		int argumentHashCode = argument.hashCode();

		Iterator<Class<?>> iterator = matchers.keySet().iterator();
		Class<?> keyClass1 = null, keyClass2 = null;
		int hashCode1 = 0, hashCode2 = 0;
		boolean equals1 = false, equals2 = false;
		if (iterator.hasNext()) {
			keyClass1 = iterator.next();
			hashCode1 = keyClass1.hashCode();
			equals1 = keyClass1.equals(argumentClass);
		}
		if (iterator.hasNext()) {
			keyClass2 = iterator.next();
			hashCode2 = keyClass2.hashCode();
			equals2 = keyClass2.equals(argumentClass);
		}
		System.out.println("" + keyClass1 + keyClass2 + hashCode1 + hashCode2 + equals1 + equals2);
	}

	// TODO: Remove debug aid
	private void debugAid2(Object argument, Map<Object, Queue<MatchingArgument>> matchersForClass) {
		Object key = matchersForClass.keySet().toArray(new Object[0])[0];
		Class<?> clazz = argument.getClass();
		int hashCode1 = argument.hashCode();
		int hashCode2 = key.hashCode();
		boolean equals = key.equals(argument);
	}

	public <S> String getMethodName(Callable<S> callableMethodInvoker) {
		return getMethod(callableMethodInvoker).getName();
	}

	public String getMethodName(ThrowingRunnable runnableMethodInvoker) {
		return getMethod(runnableMethodInvoker).getName();
	}

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

	public <S> S storeAndCreateIdInstanceOfTypeArgument(Predicate<S> predicate, Class<S> clazz) {
		return storeAndCreateIdInstanceOfTypeArgument(predicate, clazz, Optional.empty());
	}

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
		identifierValues.add(identifierValue);
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

}
