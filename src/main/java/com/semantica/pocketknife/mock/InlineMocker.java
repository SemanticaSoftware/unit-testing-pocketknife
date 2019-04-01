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
import com.semantica.pocketknife.calls.CallsFactory;
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
 * @author A. Haanstra
 *
 */
public class InlineMocker {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InlineMocker.class);

	// key: tuple of class of delegated interface and mock instance
	private final Map<Tuple<Class<?>, ?>, Object> delegates = new HashMap<>();

	private final Map<Object, Class<?>> proxyToMockedInterface = new HashMap<>();
	// key: mock interface class (we just need one MethodRecorder per mock class)
	private final Map<Class<?>, MockMethodRecorder<?>> methodRecorders = new HashMap<>();

	private final MockVerificationStore mockVerificationStore;
	private final CapturedMatchersStore capturedMatchersStore;
	private final InterceptionsStore interceptionsStore;
	private final CallRegistriesStore<? extends Calls<Method>> callRegistriesStore;
	private final ExactToMatchingMethodConverter exactToMatchingMethodConverter;

	private Stubber<?> stubber;
	private AlternativeStubber<?> alternativeStubber;
	private PreparedProxyState preparedProxyState = PreparedProxyState.MOCKING_ON_INTERCEPT;

	private InvocationHandler handler = new CallHandler();

	private enum PreparedProxyState {
		STUBBING_ON_INTERCEPT, VERIFICATION_ON_INTERCEPT, MOCKING_ON_INTERCEPT;
	}

	public interface ExactToMatchingMethodConverter {

		public <S> void register(Class<S> clazz, S proxy);

		public QualifiedMethodCall<Method> convert(QualifiedMethodCall<Method> qualifiedMethodCall);

	}

	public static interface CallRegistriesStore<T extends Calls<Method>> {
		public <S> void newCallsRegistryFor(Object proxy);

		public void removeCall(QualifiedMethodCall<Method> qualifiedMethodCall);

		public void assertCalled(Invoked numberOfTimesIncomingMethodIsExpectedToBeInvoked,
				QualifiedMethodCall<Method> qualifiedMatchingMethod);

		public void registerCall(QualifiedMethodCall<Method> qualifiedMethodCall);

		public void assertNoMoreMethodInvocations(Object... mocks);

		public void assertNoMoreMethodInvocationsAnywhere();
	}

	public static interface InterceptionsStore {
		public void addInterceptions(QualifiedMethodCall<Method> qualifiedMethodCall, List<Object> returnValues);

		public Optional<Object> matchExactMethodCallToStoredMatchingMethodCalls(
				QualifiedMethodCall<Method> qualifiedMethodCall);
	}

	public static interface CapturedMatchersStore {
		public Iterator<MatcherCapture<?>> getMatcherCapturesIterator();

		public <T> void storeMatcherCapture(Object matcher, Class<T> clazz, Optional<Integer> argumentNumber,
				T wiringIdentity);

		public void clearMatcherCaptures();
	}

	public static interface MockVerificationStore extends CapturedMatchersStore {
		public void setNumberOfTimesIncomingMethodIsExpectedToBeInvoked(Invoked timesInvoked);

		public Invoked getNumberOfTimesIncomingMethodIsExpectedToBeInvoked();

		public void clearNumberOfTimesIncomingMethodIsExpectedToBeInvoked();
	}

	/**
	 * Factory class for InlineMocker
	 *
	 */
	public static class InlineMockers {
		private static final DefaultCalls<Method> DUMMY_DEFAULT_CALLS = new DefaultMockCallsRegistry<>(Method.class);
		private static final StrictCalls<Method> DUMMY_STRICT_CALLS = new StrictMockCallsRegistry<>(Method.class);

		public static InlineMocker get(CallsFactory.CallType callType) {
			return new InlineMocker(mockVerificationStore(), capturedMatchersStore(), interceptionsStore(),
					callRegistriesStore(callType), exactToMatchingMethodConverter());
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

		@SuppressWarnings("unchecked")
		private static CallRegistriesStore<? extends Calls<Method>> callRegistriesStore(
				CallsFactory.CallType callType) {
			switch (callType) {
			case DEFAULT:
				return new InlineMockerCallRegistriesStore<DefaultCalls<Method>>(
						(Class<DefaultCalls<Method>>) DUMMY_DEFAULT_CALLS.getClass());
			case STRICT:
				return new InlineMockerCallRegistriesStore<StrictCalls<Method>>(
						(Class<StrictCalls<Method>>) DUMMY_STRICT_CALLS.getClass());
			default:
				throw new NotImplementedException(String.format("Unknown CallType: %s.", callType));
			}
		}

		private static ExactToMatchingMethodConverter exactToMatchingMethodConverter() {
			return new InlineMockerMethodConverter(capturedMatchersStore());
		}
	}

	private InlineMocker(MockVerificationStore mockVerificationStore, CapturedMatchersStore capturedMatchersStore,
			InterceptionsStore interceptionsStore, CallRegistriesStore<? extends Calls<Method>> callRegistriesStore,
			ExactToMatchingMethodConverter exactToMatchingMethodConverter) {
		this.mockVerificationStore = mockVerificationStore;
		this.capturedMatchersStore = capturedMatchersStore;
		this.interceptionsStore = interceptionsStore;
		this.callRegistriesStore = callRegistriesStore;
		this.exactToMatchingMethodConverter = exactToMatchingMethodConverter;
	}

	@SuppressWarnings("unchecked")
	public <S> S mock(Class<S> clazz) {
		S proxy = (S) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { clazz }, handler);
		exactToMatchingMethodConverter.register(clazz, proxy);
		callRegistriesStore.newCallsRegistryFor(proxy);
		return proxy;
	}

	public <S> Stubber<S> whenIntercepted(S dummy) {
		undoCallRegistrationDuringUnpreparedStubbing();
		return stubber.typeParameterize();
	}

	private void undoCallRegistrationDuringUnpreparedStubbing() {
		callRegistriesStore.removeCall(stubber.getQualifiedMethodCall());

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
		mockVerificationStore.setNumberOfTimesIncomingMethodIsExpectedToBeInvoked(timesInvoked);
		InlineMocker.this.preparedProxyState = PreparedProxyState.VERIFICATION_ON_INTERCEPT;
		return mock;
	}

	public <S> S assertCalled(S mock) {
		return assertCalled(Invoked.ONCE, mock);
	}

	public void assertNoMoreMethodInvocations(Object... mocks) {
		callRegistriesStore.assertNoMoreMethodInvocations(mocks);

	}

	public void assertNoMoreMethodInvocationsAnywhere() {
		callRegistriesStore.assertNoMoreMethodInvocationsAnywhere();

	}

	private void addInterceptions(QualifiedMethodCall<Method> qualifiedMethodCall, Object returnValue,
			Object... returnValues) {
		addInterceptions(qualifiedMethodCall, TestUtils.toList(returnValue, returnValues));
	}

	private void addInterceptions(QualifiedMethodCall<Method> qualifiedMethodCall, List<Object> returnValues) {
		qualifiedMethodCall = exactToMatchingMethodConverter.convert(qualifiedMethodCall);
		interceptionsStore.addInterceptions(qualifiedMethodCall, returnValues);
		mockVerificationStore.clearMatcherCaptures();
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

		public QualifiedMethodCall<Method> getQualifiedMethodCall() {
			return qualifiedMethodCall;
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

			switch (InlineMocker.this.preparedProxyState) {
			case STUBBING_ON_INTERCEPT: // mocker.doReturn(retVal).when(mock).someMethod();
				InlineMocker.this.preparedProxyState = PreparedProxyState.MOCKING_ON_INTERCEPT;
				return stub(qualifiedMethodCall);
			case VERIFICATION_ON_INTERCEPT: // mocker.assertCalled(Invoked.ONCE, mock).someMethod(someArg);
				InlineMocker.this.preparedProxyState = PreparedProxyState.MOCKING_ON_INTERCEPT;
				return verifyAndRemoveCall(qualifiedMethodCall);
			case MOCKING_ON_INTERCEPT: // mock.someMethod();
				// Start stubbing in case this intercept was executed as part of
				// mocker.whenIntercepted(mock.someMethod()).thenReturn(retVal);
				stubber = new Stubber<>(qualifiedMethodCall);
				// Register this proxy method invocation for later verification (needs to be
				// removed if stubbing proceeds from instantiated stubber): see
				// mocker.whenIntercepted(..) and undoCallRegistrationDuringUnpreparedStubbing()
				callRegistriesStore.registerCall(qualifiedMethodCall);
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

		private Object verifyAndRemoveCall(QualifiedMethodCall<Method> qualifiedMethodCall)
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
			QualifiedMethodCall<Method> qualifiedMatchingMethod = new QualifiedMethodCall<>(inlineMockerProxy,
					matchingMethod);

			callRegistriesStore.assertCalled(
					mockVerificationStore.getNumberOfTimesIncomingMethodIsExpectedToBeInvoked(),
					qualifiedMatchingMethod);

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
