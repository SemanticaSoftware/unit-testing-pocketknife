package com.semantica.pocketknife.mock.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import com.semantica.pocketknife.calls.MethodCall;
import com.semantica.pocketknife.mock.ExactToMatchingMethodConverter;
import com.semantica.pocketknife.mock.dto.MatcherCapture;
import com.semantica.pocketknife.mock.dto.QualifiedMethodCall;
import com.semantica.pocketknife.mock.service.support.components.DynamicMockingMethodRecorder;

public class InlineMockerMethodConverter implements ExactToMatchingMethodConverter {

	private final Map<Object, Class<?>> proxyToMockedInterface = new HashMap<>();
	// key: mock interface class (we just need one MethodRecorder per mock class)
	private final Map<Class<?>, DynamicMockingMethodRecorder<?>> methodRecorders = new HashMap<>();

	private final CapturedMatchersStore matchersUsedInConversionStore;

	public static interface CapturedMatchersStore {
		public Iterator<MatcherCapture<?>> getMatcherCapturesIterator();

		public <T> void storeMatcherCapture(Object matcher, Class<T> clazz, Optional<Integer> argumentNumber,
				T wiringIdentity);

		public void clearMatcherCaptures();
	}

	public InlineMockerMethodConverter(CapturedMatchersStore matchersUsedInConversionStore) {
		super();
		this.matchersUsedInConversionStore = matchersUsedInConversionStore;
	}

	@Override
	public <S> void register(Class<S> clazz, S proxy) {
		proxyToMockedInterface.put(proxy, clazz);
		if (methodRecorders.get(clazz) == null) {
			methodRecorders.put(clazz, new DynamicMockingMethodRecorder<>(clazz));
		}
	}

	@Override
	public QualifiedMethodCall<Method> convert(QualifiedMethodCall<Method> qualifiedMethodCall) {
		Object proxy = qualifiedMethodCall.getInvokedOnInstance();
		MethodCall<Method> methodCall = qualifiedMethodCall.getMethodCall();
		Iterator<MatcherCapture<?>> matcherCaptures = matchersUsedInConversionStore.getMatcherCapturesIterator();
		// Propagate the stored matchers to the methodRecorder
		DynamicMockingMethodRecorder<?> methodRecorder = setupMethodRecorderWithMatchers(proxy, matcherCaptures);
		try {
			// Invoke the method on its proxy
			MethodCall<Method> methodCallWithMatchingArguments = methodRecorder
					.getMethodCall(methodCall.getMethod().invoke(methodRecorder.getProxy(), methodCall.getArgs()));
			return new QualifiedMethodCall<>(proxy, methodCallWithMatchingArguments);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException("Could not invoke method with reflection.", e);
		} finally {
			matchersUsedInConversionStore.clearMatcherCaptures();
		}
	}

	/*
	 * Invoke the caught matchingstates as if
	 * storeAndCreateIdInstanceOfTypeArgument(..) was invoked on the method recorder
	 * while a method was invoked on its proxy [convert(..)].
	 */
	private DynamicMockingMethodRecorder<?> setupMethodRecorderWithMatchers(Object proxy,
			Iterator<MatcherCapture<?>> matcherCaptures) {
		DynamicMockingMethodRecorder<?> methodRecorder = methodRecorders.get(proxyToMockedInterface.get(proxy));
		while (matcherCaptures.hasNext()) {
			MatcherCapture<?> matcherCapture = matcherCaptures.next();
			methodRecorder.storeMatcherWithCastedIdInstanceOfTypeArgumentAsKey(matcherCapture.getMatcher(),
					matcherCapture.getClazz(), matcherCapture.getArgumentNumber(), matcherCapture.getWiringIdentity());
		}
		return methodRecorder;
	}

	@Override
	public <T> void storeMatcherCapture(Object matcher, Class<T> clazz, Optional<Integer> argumentNumber,
			T wiringIdentity) {
		matchersUsedInConversionStore.storeMatcherCapture(matcher, clazz, argumentNumber, wiringIdentity);
	}
}
