package com.semantica.pocketknife.mock;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Predicate;

import org.apache.commons.lang3.NotImplementedException;
import org.hamcrest.Matcher;

import com.semantica.pocketknife.calls.Calls;
import com.semantica.pocketknife.calls.CallsUtils;
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
 * TODO: This class needs to be much refactored. Easier construction is needed.
 * Should create calls on construction itself. Split up intercept method. Needs
 * unit testing.
 *
 * TODO: * easier instantiation * accepting matchers
 *
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
	private final Map<Object, Map<MethodCall<Method>, Queue<Object>>> allInterceptions = new HashMap<>();
	private final Map<Object, T> allCallsRegistries = new HashMap<>();
	// key: tuple of class of delegated interface and mock instance
	private final Map<Tuple<Class<?>, ?>, Object> delegates = new HashMap<>();

	// key: mock class (we just need one MethodRecorder per mock class)
	private final Map<Object, Class<?>> proxyToMockedInterface = new HashMap<>();
	private final Map<Class<?>, MockMethodRecorder<?>> methodRecorders = new HashMap<>();

	private final MockVerificationStore mockVerificationStore;
	private final CapturedMatchersStore capturedMatchersStore;

	private Stubber<?> stubber;
	private AlternativeStubber<?> alternativeStubber;
	private PreparedProxyState preparedProxyState = PreparedProxyState.MOCKING_ON_INTERCEPT;

	private InvocationHandler handler = new CallHandler();

	private enum PreparedProxyState {
		STUBBING_ON_INTERCEPT, VERIFICATION_ON_INTERCEPT, MOCKING_ON_INTERCEPT;
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
					mockVerificationStore(), capturedMatchersStore());
		}

		@SuppressWarnings("unchecked")
		public static InlineMocker<StrictCalls<Method>> getStrict() {
			return new InlineMocker<>((Class<StrictCalls<Method>>) DUMMY_STRICT_CALLS.getClass(),
					mockVerificationStore(), capturedMatchersStore());
		}

		private static MockVerificationStore mockVerificationStore() {
			return new InlineMockerMockStore();
		}

		private static CapturedMatchersStore capturedMatchersStore() {
			return new InlineMockerMockStore();
		}
	}

	private InlineMocker(Class<T> callsClass, MockVerificationStore mockVerificationStore,
			CapturedMatchersStore capturedMatchersStore) {
		this.callsClass = callsClass;
		this.mockVerificationStore = mockVerificationStore;
		this.capturedMatchersStore = capturedMatchersStore;
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

	private void addInterceptions(Object proxy, MethodCall<Method> methodCall, Object returnValue,
			Object... returnValues) {
		addInterceptions(proxy, methodCall, TestUtils.toList(returnValue, returnValues));
	}

	private void addInterceptions(Object proxy, MethodCall<Method> methodCall, List<Object> returnValues) {
		// overwrite method call with call with matchers
		Iterator<MatcherCapture<?>> matcherCaptures = capturedMatchersStore.getMatcherCapturesIterator();
		MockMethodRecorder<?> methodRecorder = setupMethodRecorderWithMatchers(proxy, methodCall, matcherCaptures);
		try {
			methodCall = methodRecorder
					.getMethodCall(methodCall.getMethod().invoke(methodRecorder.getProxy(), methodCall.getArgs()));
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException("Could not invoke method with reflection.", e);
		}

		Map<MethodCall<Method>, Queue<Object>> interceptions = allInterceptions.get(proxy);
		if (interceptions == null) {
			interceptions = new HashMap<>();
			InlineMocker.this.allInterceptions.put(proxy, interceptions);
		}
		Queue<Object> orderedReturnValues = interceptions.get(proxy);
		if (orderedReturnValues == null) {
			orderedReturnValues = new ArrayDeque<>(returnValues.size());
			interceptions.put(methodCall, orderedReturnValues);
		}
		for (Object returnValue : returnValues) {
			orderedReturnValues.add(returnValue);
		}
	}

	private MockMethodRecorder<?> setupMethodRecorderWithMatchers(Object proxy, MethodCall<Method> methodCall,
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

		private Object proxy;
		private MethodCall<Method> methodCall;

		private Stubber(Object proxy, MethodCall<Method> methodCall) {
			this.proxy = proxy;
			this.methodCall = methodCall;
		}

		/**
		 * Returns the given {@code returnValues} from the mock in the same order as in
		 * the {@link List} for consecutive method calls on the mock.
		 *
		 * @param returnValues
		 */
		public Stubber<U> thenReturn(U returnValue, @SuppressWarnings("unchecked") U... returnValues) {
			addInterceptions(proxy, methodCall, returnValue, returnValues);
			return this;
		}

		public Stubber<U> thenReturn(U returnValue, Return returnTimes) {
			addInterceptions(proxy, methodCall, TestUtils.fillList(returnValue, returnTimes.getTimes()));
			return this;
		}

		private <V> Stubber<V> typeParameterize() {
			return new Stubber<>(proxy, methodCall);
		}

		private Object getProxy() {
			return proxy;
		}

		private MethodCall<Method> getMethodCall() {
			return methodCall;
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
			Optional<Object> proxyRelatedReturnValue = toStringHashCodeEquals(proxy, methodCall);
			if (proxyRelatedReturnValue.isPresent()) {
				return proxyRelatedReturnValue.get();
			}

			T calls = allCallsRegistries.get(proxy);
			switch (InlineMocker.this.preparedProxyState) {
			case STUBBING_ON_INTERCEPT: // mocker.doReturn(retVal).when(mock).someMethod();
				InlineMocker.this.preparedProxyState = PreparedProxyState.MOCKING_ON_INTERCEPT;
				return stub(proxy, methodCall);
			case VERIFICATION_ON_INTERCEPT: // mocker.assertCalled(Invoked.ONCE, mock).someMethod(someArg);
				InlineMocker.this.preparedProxyState = PreparedProxyState.MOCKING_ON_INTERCEPT;
				return verifyAndRemoveCall(proxy, methodCall, calls);
			case MOCKING_ON_INTERCEPT: // mock.someMethod();
				// Start stubbing in case this intercept was executed as part of
				// mocker.whenIntercepted(mock.someMethod()).thenReturn(retVal);
				stubber = new Stubber<>(proxy, methodCall);
				// Register this proxy method invocation for later verification (needs to be
				// removed if stubbing proceeds from instantiated stubber): see
				// mocker.whenIntercepted(..) and undoCallRegistrationDuringUnpreparedStubbing()
				calls.registerCall(methodCall);
				Optional<Object> stubReturnValue = executeStub(proxy, methodCall);
				return stubReturnValue.orElseGet(() -> executeDelegate(proxy, methodCall)
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

		private Object stub(Object proxy, MethodCall<Method> methodCall) {
			addInterceptions(proxy, methodCall, InlineMocker.this.alternativeStubber.getReturnValues());
			return DefaultValues.defaultValue(methodCall.getMethod().getReturnType());
		}

		private Object verifyAndRemoveCall(Object proxy, MethodCall<Method> methodCall, T calls)
				throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			// Invoke the caught matchingstates as if
			// storeAndCreateIdInstanceOfTypeArgument(..) was invoked on the method recorder
			// while a method was invoked on its proxy
			Iterator<MatcherCapture<?>> matcherCaptures = mockVerificationStore.getMatcherCapturesIterator();
			MockMethodRecorder<?> methodRecorder = setupMethodRecorderWithMatchers(proxy, methodCall, matcherCaptures);
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

		private Optional<Object> executeStub(Object proxy, MethodCall<Method> methodCall) {

			// query in calls is matching method agains multiple exact methods
			// now query in mocker is exact method agains multiple matching methods --> add
			// MockUtils.getStoredMatchingMethodCall and call this method instead of
			// CallsUtils.getStoredExactMethodCall(..)
			Map<MethodCall<Method>, Queue<Object>> interceptions = allInterceptions.get(proxy);
			MethodCall<Method> exactMethodCall = CallsUtils.getStoredExactMethodCall(methodCall,
					interceptions.keySet());

			if (interceptions != null) {
				if (interceptions.containsKey(exactMethodCall)) {
					Object returnValue = interceptions.get(exactMethodCall).poll();
					return Optional.ofNullable(returnValue);
				}
			}
			return Optional.empty();
		}

		/*
		 * Both the mock and delegate are instances of the interface (see delegate(..)
		 * method). We only want to delegate methods in the interface. If the current
		 * method is in the interface, the declaring class is the interface. Therefore,
		 * we request a tuple key with this interface and proxy, and invoke the delegate
		 * if it has been set (not null).
		 *
		 */
		private Optional<Object> executeDelegate(Object proxy, MethodCall<Method> methodCall) {
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
