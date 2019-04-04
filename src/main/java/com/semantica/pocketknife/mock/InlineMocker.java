package com.semantica.pocketknife.mock;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.lang3.NotImplementedException;
import org.hamcrest.Matcher;

import com.semantica.pocketknife.calls.Calls;
import com.semantica.pocketknife.calls.Invoked;
import com.semantica.pocketknife.calls.MethodCall;
import com.semantica.pocketknife.calls.Return;
import com.semantica.pocketknife.methodrecorder.DefaultValues;
import com.semantica.pocketknife.methodrecorder.RandomIdentifierValues;
import com.semantica.pocketknife.mock.dto.QualifiedMethodCall;
import com.semantica.pocketknife.util.TestUtils;

/**
 * Minimalistic dynamic mock creator class.
 *
 * @author A. Haanstra
 *
 */
public class InlineMocker {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InlineMocker.class);

	private final InterceptionsStore interceptionsStore;
	private final DelegatesStore delegatesStore;
	private final VerificationStore<? extends Calls<Method>> verificationStore;
	private final ExactToMatchingMethodConverter exactToMatchingMethodConverter;

	private Stubber<?> stubber;
	private AlternativeStubber<?> alternativeStubber;
	private PreparedProxyState preparedProxyState = PreparedProxyState.MOCKING_ON_INTERCEPT;
	private InvocationHandler handler = new CallHandler();

	InlineMocker(InterceptionsStore interceptionsStore, DelegatesStore delegatesStore,
			VerificationStore<? extends Calls<Method>> callRegistriesStore,
			ExactToMatchingMethodConverter exactToMatchingMethodConverter) {
		this.interceptionsStore = interceptionsStore;
		this.verificationStore = callRegistriesStore;
		this.exactToMatchingMethodConverter = exactToMatchingMethodConverter;
		this.delegatesStore = delegatesStore;
	}

	@SuppressWarnings("unchecked")
	public <S> S mock(Class<S> clazz) {
		S proxy = (S) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, handler);
		exactToMatchingMethodConverter.register(clazz, proxy);
		verificationStore.newCallsRegistryFor(proxy);
		return proxy;
	}

	public <S> Stubber<S> whenIntercepted(S dummy) {
		undoCallRegistrationDuringUnpreparedStubbing();
		return stubber.typeParameterize();
	}

	private void undoCallRegistrationDuringUnpreparedStubbing() {
		verificationStore.removeCall(stubber.getQualifiedMethodCall());

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
		delegatesStore.register(interfaze, mock, delegate);
	}

	public <S> S assertCalled(Invoked timesInvoked, S mock) {
		verificationStore.addNumberOfTimesIncomingMethodIsExpectedToBeInvoked(timesInvoked);
		preparedProxyState = PreparedProxyState.VERIFICATION_ON_INTERCEPT;
		return mock;
	}

	public <S> S assertCalled(S mock) {
		return assertCalled(Invoked.ONCE, mock);
	}

	public void assertNoMoreMethodInvocations(Object... mocks) {
		verificationStore.assertNoMoreMethodInvocations(mocks);

	}

	public void assertNoMoreMethodInvocationsAnywhere() {
		verificationStore.assertNoMoreMethodInvocationsAnywhere();

	}

	private void addInterceptions(QualifiedMethodCall<Method> qualifiedMethodCall, Object returnValue,
			Object... returnValues) {
		addInterceptions(qualifiedMethodCall, TestUtils.toList(returnValue, returnValues));
	}

	private void addInterceptions(QualifiedMethodCall<Method> qualifiedMethodCall, List<Object> returnValues) {
		qualifiedMethodCall = exactToMatchingMethodConverter.convert(qualifiedMethodCall);
		interceptionsStore.addInterceptions(qualifiedMethodCall, returnValues);
	}

	public <S> S matchArgWith(Predicate<S> predicate, Class<S> clazz) {
		S wiringIdentity = RandomIdentifierValues.identifierValue(clazz);
		exactToMatchingMethodConverter.storeMatcherCapture(predicate, clazz, Optional.empty(), wiringIdentity);
		return wiringIdentity;
	}

	public <S> S matchArgWith(Matcher<S> matcher, Class<S> clazz) {
		S wiringIdentity = RandomIdentifierValues.identifierValue(clazz);
		exactToMatchingMethodConverter.storeMatcherCapture(matcher, clazz, Optional.empty(), wiringIdentity);
		return wiringIdentity;
	}

	public <S> S matchArgWith(Predicate<S> predicate, Class<S> clazz, int argumentNumber) {
		S wiringIdentity = RandomIdentifierValues.identifierValue(clazz);
		exactToMatchingMethodConverter.storeMatcherCapture(predicate, clazz, Optional.of(argumentNumber),
				wiringIdentity);
		return wiringIdentity;
	}

	public <S> S matchArgWith(Matcher<S> matcher, Class<S> clazz, int argumentNumber) {
		S wiringIdentity = RandomIdentifierValues.identifierValue(clazz);
		exactToMatchingMethodConverter.storeMatcherCapture(matcher, clazz, Optional.of(argumentNumber), wiringIdentity);
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
			InlineMocker.this.addInterceptions(this.qualifiedMethodCall, returnValue, returnValues);
			return this;
		}

		public Stubber<U> thenReturn(U returnValue, Return returnTimes) {
			InlineMocker.this.addInterceptions(this.qualifiedMethodCall,
					TestUtils.fillList(returnValue, returnTimes.getTimes()));
			return this;
		}

		private <V> Stubber<V> typeParameterize() {
			return new Stubber<>(this.qualifiedMethodCall);
		}

		public QualifiedMethodCall<Method> getQualifiedMethodCall() {
			return this.qualifiedMethodCall;
		}

	}

	private enum PreparedProxyState {
		STUBBING_ON_INTERCEPT, VERIFICATION_ON_INTERCEPT, MOCKING_ON_INTERCEPT;
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
				verificationStore.registerCall(qualifiedMethodCall);
				Optional<Object> stubReturnValue = executeStub(qualifiedMethodCall);
				return stubReturnValue.orElseGet(() -> delegatesStore.executeDelegate(qualifiedMethodCall)
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

		private Object verifyAndRemoveCall(QualifiedMethodCall<Method> qualifiedExactMethodCall)
				throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			QualifiedMethodCall<Method> qualifiedMatchingMethod = exactToMatchingMethodConverter
					.convert(qualifiedExactMethodCall);
			verificationStore.assertCalled(qualifiedMatchingMethod);
			return DefaultValues.defaultValue(qualifiedExactMethodCall.getMethodCall().getMethod().getReturnType());
		}

		private Optional<Object> executeStub(QualifiedMethodCall<Method> qualifiedMethodCall) {
			return interceptionsStore.matchExactMethodCallToStoredMatchingMethodCalls(qualifiedMethodCall);
		}

	}

}
