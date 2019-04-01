package com.semantica.pocketknife.mock;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.lang3.NotImplementedException;
import org.hamcrest.Matcher;

import com.semantica.pocketknife.calls.Calls;
import com.semantica.pocketknife.calls.DefaultCalls;
import com.semantica.pocketknife.calls.Invoked;
import com.semantica.pocketknife.calls.MethodCall;
import com.semantica.pocketknife.calls.Return;
import com.semantica.pocketknife.calls.StrictCalls;
import com.semantica.pocketknife.methodrecorder.DefaultValues;
import com.semantica.pocketknife.methodrecorder.RandomIdentifierValues;
import com.semantica.pocketknife.util.TestUtils;
import com.semantica.pocketknife.util.Tuple;

/**
 * Minimalistic dynamic mock creator class.
 *
 * This class was adapted and modified from
 * https://github.com/brocchini/DIY-mock
 *
 * @author A. Haanstra, M. Brocchini,
 *
 */
public class InlineMocker<T extends Calls<Method>> {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InlineMocker.class);

	private final Class<T> callsClass;
	// key: the proxy (mock) instance
	private final Map<Object, T> allCallsRegistries = new HashMap<>();
	// key: tuple of class of delegated interface and mock instance
	private final Map<Tuple<Class<?>, ?>, Object> delegates = new HashMap<>();

	private final Map<Object, Class<?>> proxyToMockedInterface = new HashMap<>();
	// key: mock interface class (we just need one MethodRecorder per mock class)
	private final Map<Class<?>, MockMethodRecorder<?>> methodRecorders = new HashMap<>();

	private final MockVerificationStore mockVerificationStore;
	private final CapturedMatchersStore capturedMatchersStore;
	private final InterceptionsStore interceptionsStore;

	private Stubber<?> stubber;
	private AlternativeStubber<?> alternativeStubber;
	private PreparedProxyState preparedProxyState = PreparedProxyState.MOCKING_ON_INTERCEPT;

	private InvocationHandler handler = new CallHandler();

	private enum PreparedProxyState {
		STUBBING_ON_INTERCEPT, VERIFICATION_ON_INTERCEPT, MOCKING_ON_INTERCEPT;
	}

	public static interface InterceptionsStore {

		void addInterceptions(QualifiedMethodCall<Method> qualifiedMethodCall, List<Object> returnValues);

		Optional<Object> matchExactMethodCallToStoredMatchingMethodCalls(
				QualifiedMethodCall<Method> qualifiedMethodCall);

	}

	public static interface CapturedMatchersStore {
		public abstract Iterator<MatcherCapture<?>> getMatcherCapturesIterator();

		public abstract <T> void storeMatcherCapture(Object matcher, Class<T> clazz, Optional<Integer> argumentNumber,
				T wiringIdentity);

		public abstract void clearMatcherCaptures();
	}

	public static interface MockVerificationStore extends CapturedMatchersStore {
		public abstract void setNumberOfTimesIncomingMethodIsExpectedToBeInvoked(Invoked timesInvoked);

		public abstract Invoked getNumberOfTimesIncomingMethodIsExpectedToBeInvoked();

		public abstract void clearNumberOfTimesIncomingMethodIsExpectedToBeInvoked();
	}

	/**
	 * Factory class for InlineMocker
	 *
	 */
	public static class InlineMockers {
		private static final DefaultCalls<Method> DUMMY_DEFAULT_CALLS = new DefaultMockCallsRegistry<>(Method.class);
		private static final StrictCalls<Method> DUMMY_STRICT_CALLS = new StrictMockCallsRegistry<>(Method.class);

		@SuppressWarnings("unchecked")
		public static InlineMocker<DefaultCalls<Method>> getDefault() {
			return new InlineMocker<>((Class<DefaultCalls<Method>>) DUMMY_DEFAULT_CALLS.getClass(),
					mockVerificationStore(), capturedMatchersStore(), interceptionsStore());
		}

		@SuppressWarnings("unchecked")
		public static InlineMocker<StrictCalls<Method>> getStrict() {
			return new InlineMocker<>((Class<StrictCalls<Method>>) DUMMY_STRICT_CALLS.getClass(),
					mockVerificationStore(), capturedMatchersStore(), interceptionsStore());
		}

		private static MockVerificationStore mockVerificationStore() {
			return new InlineMockerMockStore();
		}

		private static CapturedMatchersStore capturedMatchersStore() {
			return new InlineMockerMockStore();
		}

		private static InterceptionsStore interceptionsStore() {
			return new InlineMockerInterceptionsStore();
		}
	}

	private InlineMocker(Class<T> callsClass, MockVerificationStore mockVerificationStore,
			CapturedMatchersStore capturedMatchersStore, InterceptionsStore interceptionsStore) {
		this.callsClass = callsClass;
		this.mockVerificationStore = mockVerificationStore;
		this.capturedMatchersStore = capturedMatchersStore;
		this.interceptionsStore = interceptionsStore;
	}

	@SuppressWarnings("unchecked")
	public <S> S mock(Class<S> clazz) {
		S proxy = (S) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { clazz }, handler);
		proxyToMockedInterface.put(proxy, clazz);
		if (methodRecorders.get(clazz) == null) {
			methodRecorders.put(clazz, new MockMethodRecorder<>(clazz));
		}
		T calls = null;
		if (DefaultCalls.class.isAssignableFrom(callsClass)) {
			calls = (T) new DefaultMockCallsRegistry<>(Method.class);
		} else if (StrictCalls.class.isAssignableFrom(callsClass)) {
			calls = (T) new StrictMockCallsRegistry<>(Method.class);
		} else {
			throwNotImplementedExceptionForCallsClass();
		}
		allCallsRegistries.put(proxy, calls);
		return proxy;
	}

	public <S> Stubber<S> whenIntercepted(S dummy) {
		undoCallRegistrationDuringUnpreparedStubbing();
		return stubber.typeParameterize();
	}

	private void undoCallRegistrationDuringUnpreparedStubbing() {
		Calls<Method> calls = allCallsRegistries.get(stubber.getProxy());
		calls.removeCall(stubber.getMethodCall());
	}

	@SafeVarargs
	public final <S> AlternativeStubber<S> doReturn(S returnValue, S... returnValues) {
		this.preparedProxyState = PreparedProxyState.STUBBING_ON_INTERCEPT;
		AlternativeStubber<S> alternativeStubber = new AlternativeStubber<>(
				TestUtils.toList(returnValue, returnValues));
		this.alternativeStubber = alternativeStubber;
		return alternativeStubber;
	}

	public <S> AlternativeStubber<S> doReturn(S returnValue, Return returnTimes) {
		this.preparedProxyState = PreparedProxyState.STUBBING_ON_INTERCEPT;
		AlternativeStubber<S> alternativeStubber = new AlternativeStubber<>(
				TestUtils.fillList(returnValue, returnTimes.getTimes()));
		this.alternativeStubber = alternativeStubber;
		return alternativeStubber;
	}

	public <S> void delegate(Class<S> interfaze, S mock, S delegate) {
		if (interfaze.isInterface() && interfaze.isAssignableFrom(mock.getClass())
				&& interfaze.isAssignableFrom(delegate.getClass())) {
			Tuple<Class<?>, ?> key = new Tuple<>(interfaze, mock);
			delegates.put(key, delegate);
		} else {
			throw new IllegalArgumentException("Both mock and delegate should be a subtype of the given interface.");
		}
	}

	public <S> S assertCalled(Invoked timesInvoked, S mock) {
		if (!StrictCalls.class.isAssignableFrom(callsClass)) {
			mockVerificationStore.setNumberOfTimesIncomingMethodIsExpectedToBeInvoked(timesInvoked);
			InlineMocker.this.preparedProxyState = PreparedProxyState.VERIFICATION_ON_INTERCEPT;
			return mock;
		} else {
			throw new NotImplementedException(
					"Please use method \"verifyCall(S mock)\" when using strict call verification.");
		}
	}

	public <S> S assertCalled(S mock) {
		if (StrictCalls.class.isAssignableFrom(callsClass)) {
			InlineMocker.this.preparedProxyState = PreparedProxyState.VERIFICATION_ON_INTERCEPT;
			return mock;
		} else {
			throw new NotImplementedException(
					"Please use method \"verifyCall(Invoked timesInvoked, S mock)\" when using non-strict call verification.");
		}
	}

	public void assertNoMoreMethodInvocations(Object... mocks) {
		for (Object mock : mocks) {
			assert allCallsRegistries.get(mock).verifyNoMoreMethodInvocations();
		}
	}

	public void assertNoMoreMethodInvocationsAnywhere() {
		for (T calls : allCallsRegistries.values()) {
			assert calls.verifyNoMoreMethodInvocations();
		}
	}

	private void addInterceptions(QualifiedMethodCall<Method> qualifiedMethodCall, Object returnValue,
			Object... returnValues) {
		addInterceptions(qualifiedMethodCall, TestUtils.toList(returnValue, returnValues));
	}

	private void addInterceptions(QualifiedMethodCall<Method> qualifiedMethodCall, List<Object> returnValues) {
		// overwrite method call with call with matchers
		Object proxy = qualifiedMethodCall.getInvokedOnInstance();
		MethodCall<Method> methodCall = qualifiedMethodCall.getMethodCall();
		Iterator<MatcherCapture<?>> matcherCaptures = capturedMatchersStore.getMatcherCapturesIterator();
		MockMethodRecorder<?> methodRecorder = setupMethodRecorderWithMatchers(proxy, matcherCaptures);
		try {
			MethodCall<Method> methodCallWithMatchingArguments = methodRecorder
					.getMethodCall(methodCall.getMethod().invoke(methodRecorder.getProxy(), methodCall.getArgs()));
			qualifiedMethodCall = new QualifiedMethodCall<>(proxy, methodCallWithMatchingArguments);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException("Could not invoke method with reflection.", e);
		}

		interceptionsStore.addInterceptions(qualifiedMethodCall, returnValues);

	}

	private MockMethodRecorder<?> setupMethodRecorderWithMatchers(Object proxy,
			Iterator<MatcherCapture<?>> matcherCaptures) {
		MockMethodRecorder<?> methodRecorder = methodRecorders.get(proxyToMockedInterface.get(proxy));
		while (matcherCaptures.hasNext()) {
			MatcherCapture<?> matcherCapture = matcherCaptures.next();
			methodRecorder.storeMatcherWithCastedIdInstanceOfTypeArgumentAsKey(matcherCapture.getMatcher(),
					matcherCapture.getClazz(), matcherCapture.getArgumentNumber(), matcherCapture.getWiringIdentity());
		}
		mockVerificationStore.clearMatcherCaptures();
		return methodRecorder;
	}

	private void throwNotImplementedExceptionForCallsClass() {
		throw new NotImplementedException(
				String.format("Calls class %s is unknown and not implemented for %s.", callsClass, this.getClass()));
	}

	public <S> S matchArgWith(Predicate<S> predicate, Class<S> clazz) {
		S wiringIdentity = RandomIdentifierValues.identifierValue(clazz);
		mockVerificationStore.storeMatcherCapture(predicate, clazz, Optional.empty(), wiringIdentity);
		capturedMatchersStore.storeMatcherCapture(predicate, clazz, Optional.empty(), wiringIdentity);
		return wiringIdentity;
	}

	public <S> S matchArgWith(Matcher<S> matcher, Class<S> clazz) {
		S wiringIdentity = RandomIdentifierValues.identifierValue(clazz);
		mockVerificationStore.storeMatcherCapture(matcher, clazz, Optional.empty(), wiringIdentity);
		capturedMatchersStore.storeMatcherCapture(matcher, clazz, Optional.empty(), wiringIdentity);
		return wiringIdentity;
	}

	public <S> S matchArgWith(Predicate<S> predicate, Class<S> clazz, int argumentNumber) {
		S wiringIdentity = RandomIdentifierValues.identifierValue(clazz);
		mockVerificationStore.storeMatcherCapture(predicate, clazz, Optional.of(argumentNumber), wiringIdentity);
		capturedMatchersStore.storeMatcherCapture(predicate, clazz, Optional.of(argumentNumber), wiringIdentity);
		return wiringIdentity;
	}

	public <S> S matchArgWith(Matcher<S> matcher, Class<S> clazz, int argumentNumber) {
		S wiringIdentity = RandomIdentifierValues.identifierValue(clazz);
		mockVerificationStore.storeMatcherCapture(matcher, clazz, Optional.of(argumentNumber), wiringIdentity);
		capturedMatchersStore.storeMatcherCapture(matcher, clazz, Optional.of(argumentNumber), wiringIdentity);
		return wiringIdentity;
	}

	public class Stubber<U> {

		private QualifiedMethodCall<Method> qualifiedMethodCall;

		private Stubber(QualifiedMethodCall<Method> qualifiedMethodCall) {
			super();
			this.qualifiedMethodCall = qualifiedMethodCall;
		}

		/**
		 * Returns the given {@code returnValues} from the mock in the same order as in
		 * the {@link List} for consecutive method calls on the mock.
		 *
		 * @param returnValues
		 */
		public Stubber<U> thenReturn(U returnValue, @SuppressWarnings("unchecked") U... returnValues) {
			addInterceptions(qualifiedMethodCall, returnValue, returnValues);
			return this;
		}

		public Stubber<U> thenReturn(U returnValue, Return returnTimes) {
			addInterceptions(qualifiedMethodCall, TestUtils.fillList(returnValue, returnTimes.getTimes()));
			return this;
		}

		private <V> Stubber<V> typeParameterize() {
			return new Stubber<>(qualifiedMethodCall);
		}

		private Object getProxy() {
			return qualifiedMethodCall.getInvokedOnInstance();
		}

		private MethodCall<Method> getMethodCall() {
			return qualifiedMethodCall.getMethodCall();
		}
	}

	public class AlternativeStubber<U> {
		private final List<Object> returnValues;

		private AlternativeStubber(List<Object> returnValues) {
			super();
			this.returnValues = returnValues;
		}

		public AlternativeStubber<U> whenIntercepted(U dummy) {
			return this;
		}

		public <V> V when(V mock) {
			return mock;
		}

		private List<Object> getReturnValues() {
			return returnValues;
		}

	}

	private class CallHandler implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			MethodCall<Method> methodCall = new MethodCall<>(method, args == null ? new Object[0] : args);
			QualifiedMethodCall<Method> qualifiedMethodCall = new QualifiedMethodCall<>(proxy, methodCall);
			Optional<Object> proxyRelatedReturnValue = toStringHashCodeEquals(proxy, methodCall);
			if (proxyRelatedReturnValue.isPresent()) {
				return proxyRelatedReturnValue.get();
			}

			T calls = allCallsRegistries.get(proxy);
			switch (InlineMocker.this.preparedProxyState) {
			case STUBBING_ON_INTERCEPT: // mocker.doReturn(retVal).when(mock).someMethod();
				InlineMocker.this.preparedProxyState = PreparedProxyState.MOCKING_ON_INTERCEPT;
				return stub(qualifiedMethodCall);
			case VERIFICATION_ON_INTERCEPT: // mocker.assertCalled(Invoked.ONCE, mock).someMethod(someArg);
				InlineMocker.this.preparedProxyState = PreparedProxyState.MOCKING_ON_INTERCEPT;
				return verifyAndRemoveCall(qualifiedMethodCall, calls);
			case MOCKING_ON_INTERCEPT: // mock.someMethod();
				// Start stubbing in case this intercept was executed as part of
				// mocker.whenIntercepted(mock.someMethod()).thenReturn(retVal);
				stubber = new Stubber<>(qualifiedMethodCall);
				// Register this proxy method invocation for later verification (needs to be
				// removed if stubbing proceeds from instantiated stubber): see
				// mocker.whenIntercepted(..) and undoCallRegistrationDuringUnpreparedStubbing()
				calls.registerCall(methodCall);
				Optional<Object> stubReturnValue = executeStub(qualifiedMethodCall);
				return stubReturnValue.orElseGet(() -> executeDelegate(qualifiedMethodCall)
						.orElseGet(() -> DefaultValues.defaultValue(methodCall.getMethod().getReturnType())));
			default:
				throw new NotImplementedException(String.format("Not implemented prepared proxy state encountered: %s",
						InlineMocker.this.preparedProxyState));
			}
		}

		private Optional<Object> toStringHashCodeEquals(Object proxy, MethodCall<Method> methodCall) {
			if (methodCall.getMethod().getName().equals("hashCode") && methodCall.getArgs().length == 0) {
				return Optional.of(System.identityHashCode(proxy));
			} else if (methodCall.getMethod().getName().equals("toString") && methodCall.getArgs().length == 0) {
				return Optional.of("Mock proxy object with hashCode: " + System.identityHashCode(proxy));
			} else if (methodCall.getMethod().getName().equals("equals") && methodCall.getArgs().length == 1) {
				return Optional.of(System.identityHashCode(proxy) == System.identityHashCode(methodCall.getArgs()[0]));
			} else {
				return Optional.empty();
			}
		}

		private Object stub(QualifiedMethodCall<Method> qualifiedMethodCall) {
			addInterceptions(qualifiedMethodCall, InlineMocker.this.alternativeStubber.getReturnValues());
			return DefaultValues.defaultValue(qualifiedMethodCall.getMethodCall().getMethod().getReturnType());
		}

		private Object verifyAndRemoveCall(QualifiedMethodCall<Method> qualifiedMethodCall, T calls)
				throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			MethodCall<Method> methodCall = qualifiedMethodCall.getMethodCall();
			Object inlineMockerProxy = qualifiedMethodCall.getInvokedOnInstance();
			// Invoke the caught matchingstates as if
			// storeAndCreateIdInstanceOfTypeArgument(..) was invoked on the method recorder
			// while a method was invoked on its proxy
			Iterator<MatcherCapture<?>> matcherCaptures = mockVerificationStore.getMatcherCapturesIterator();
			MockMethodRecorder<?> methodRecorder = setupMethodRecorderWithMatchers(inlineMockerProxy, matcherCaptures);
			// Then invoke the method on its proxy
			MethodCall<Method> matchingMethod = methodRecorder
					.getMethodCall(methodCall.getMethod().invoke(methodRecorder.getProxy(), methodCall.getArgs()));
			if (DefaultCalls.class.isAssignableFrom(callsClass)) {
				assert ((DefaultCalls<Method>) calls).verifyAndRemoveCall(
						mockVerificationStore.getNumberOfTimesIncomingMethodIsExpectedToBeInvoked(), matchingMethod);
			} else if (StrictCalls.class.isAssignableFrom(callsClass)) {
				assert ((StrictCalls<Method>) calls).verifyAndRemoveCall(matchingMethod);
			} else {
				throwNotImplementedExceptionForCallsClass();
			}
			mockVerificationStore.clearNumberOfTimesIncomingMethodIsExpectedToBeInvoked();
			return DefaultValues.defaultValue(methodCall.getMethod().getReturnType());
		}

		private Optional<Object> executeStub(QualifiedMethodCall<Method> qualifiedMethodCall) {
			return interceptionsStore.matchExactMethodCallToStoredMatchingMethodCalls(qualifiedMethodCall);
		}

		/*
		 * Both the mock and delegate are instances of the interface (see delegate(..)
		 * method). We only want to delegate methods in the interface. If the current
		 * method is in the interface, the declaring class is the interface. Therefore,
		 * we request a tuple key with this interface and proxy, and invoke the delegate
		 * if it has been set (not null).
		 *
		 */
		private Optional<Object> executeDelegate(QualifiedMethodCall<Method> qualifiedMethodCall) {
			MethodCall<Method> methodCall = qualifiedMethodCall.getMethodCall();
			Object proxy = qualifiedMethodCall.getInvokedOnInstance();
			Object delegate = delegates.get(new Tuple<>(methodCall.getMethod().getDeclaringClass(), proxy));
			if (delegate != null) {
				try {
					return Optional.ofNullable(methodCall.getMethod().invoke(delegate, methodCall.getArgs()));
				} catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
					log.error("Cannot invoke delegate method {}.", methodCall.getMethod(), e.getCause());
					throw new RuntimeException(e);
				}
			}
			return Optional.empty();
		}
	}

}
