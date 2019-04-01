package com.semantica.pocketknife.mock;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.semantica.pocketknife.calls.MethodCall;
import com.semantica.pocketknife.mock.InlineMocker.CapturedMatchersStore;

public class InlineMockerMethodConverter implements InlineMocker.ExactToMatchingMethodConverter {

	private final Map<Object, Class<?>> proxyToMockedInterface = new HashMap<>();
	// key: mock interface class (we just need one MethodRecorder per mock class)
	private final Map<Class<?>, MockMethodRecorder<?>> methodRecorders = new HashMap<>();

	private final InlineMocker.CapturedMatchersStore matchersUsedInConversionStore;

	public InlineMockerMethodConverter(CapturedMatchersStore matchersUsedInConversionStore) {
		super();
		this.matchersUsedInConversionStore = matchersUsedInConversionStore;
	}

	@Override
	public <S> void register(Class<S> clazz, S proxy) {
		proxyToMockedInterface.put(proxy, clazz);
		if (methodRecorders.get(clazz) == null) {
			methodRecorders.put(clazz, new MockMethodRecorder<>(clazz));
		}
	}

	@Override
	public QualifiedMethodCall<Method> convert(QualifiedMethodCall<Method> qualifiedMethodCall) {
		Object proxy = qualifiedMethodCall.getInvokedOnInstance();
		MethodCall<Method> methodCall = qualifiedMethodCall.getMethodCall();
		Iterator<MatcherCapture<?>> matcherCaptures = matchersUsedInConversionStore.getMatcherCapturesIterator();
		MockMethodRecorder<?> methodRecorder = setupMethodRecorderWithMatchers(proxy, matcherCaptures);
		try {
			MethodCall<Method> methodCallWithMatchingArguments = methodRecorder
					.getMethodCall(methodCall.getMethod().invoke(methodRecorder.getProxy(), methodCall.getArgs()));
			return new QualifiedMethodCall<>(proxy, methodCallWithMatchingArguments);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException("Could not invoke method with reflection.", e);
		}
	}

	private MockMethodRecorder<?> setupMethodRecorderWithMatchers(Object proxy,
			Iterator<MatcherCapture<?>> matcherCaptures) {
		MockMethodRecorder<?> methodRecorder = methodRecorders.get(proxyToMockedInterface.get(proxy));
		while (matcherCaptures.hasNext()) {
			MatcherCapture<?> matcherCapture = matcherCaptures.next();
			methodRecorder.storeMatcherWithCastedIdInstanceOfTypeArgumentAsKey(matcherCapture.getMatcher(),
					matcherCapture.getClazz(), matcherCapture.getArgumentNumber(), matcherCapture.getWiringIdentity());
		}
		return methodRecorder;
	}

}
