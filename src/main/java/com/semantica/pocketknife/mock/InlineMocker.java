package com.semantica.pocketknife.mock;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;

import com.semantica.pocketknife.calls.Calls;
import com.semantica.pocketknife.calls.DefaultCalls;
import com.semantica.pocketknife.calls.Invoked;
import com.semantica.pocketknife.calls.MethodCall;
import com.semantica.pocketknife.calls.StrictCalls;
import com.semantica.pocketknife.methodrecorder.DefaultValues;
import com.semantica.pocketknife.methodrecorder.MethodRecorder;
import com.semantica.pocketknife.util.Tuple;

/**
 * TODO: This class needs to be much refactored. Easier construction is needed.
 * Should create calls on construction itself. Split up intercept method. Needs
 * unit testing.
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
	private final Map<Object, Map<MethodCall<Method>, Object>> allInterceptions = new HashMap<>();
	private final Map<Object, MethodRecorder<?>> methodRecorders = new HashMap<>();
	private final Map<Object, T> allCalls = new HashMap<>();
	// key: tuple of class of delegated interface and mock instance
	private final Map<Tuple<Class<?>, ?>, Object> delegates = new HashMap<>();

	private Stubber<?> stubber;
	private AlternativeStubber<?> alternativeStubber;
	private VerificationState verificationState = new VerificationState();
	private PreparedProxyState preparedProxyState = PreparedProxyState.MOCKING_ON_INTERCEPT;

	private InvocationHandler handler = new CallHandler();

	private enum PreparedProxyState {
		STUBBING_ON_INTERCEPT, VERIFICATION_ON_INTERCEPT, MOCKING_ON_INTERCEPT;
	}

	public static class VerificationState {
		Invoked timesInvoked = null;
	}

	public InlineMocker(Class<T> callsClass) {
		this.callsClass = callsClass;
	}

	@SuppressWarnings("unchecked")
	public <S> S mock(Class<S> clazz, T calls) {
		S proxy = (S) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { clazz }, handler);
		methodRecorders.put(proxy, MethodRecorder.recordInvocationsOn(clazz));
		allCalls.put(proxy, calls);
		return proxy;
	}

	public <S> Stubber<S> whenIntercepted(S dummy) {
		undoCallRegistrationDuringUnpreparedStubbing();
		return stubber.typeParameterize();
	}

	private void undoCallRegistrationDuringUnpreparedStubbing() {
		Calls<Method> calls = allCalls.get(stubber.getProxy());
		calls.removeCall(stubber.getMethodCall());
	}

	public <S> AlternativeStubber<S> doReturn(S returnValue) {
		this.preparedProxyState = PreparedProxyState.STUBBING_ON_INTERCEPT;
		AlternativeStubber<S> alternativeStubber = new AlternativeStubber<>(returnValue);
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
		if (callsClass != StrictCalls.class) {
			this.verificationState.timesInvoked = timesInvoked;
			InlineMocker.this.preparedProxyState = PreparedProxyState.VERIFICATION_ON_INTERCEPT;
			return mock;
		} else {
			throw new NotImplementedException(
					"Please use method \"verifyCall(S mock)\" when using strict call verification.");
		}
	}

	public <S> S assertCalled(S mock) {
		if (callsClass == StrictCalls.class) {
			InlineMocker.this.preparedProxyState = PreparedProxyState.VERIFICATION_ON_INTERCEPT;
			return mock;
		} else {
			throw new NotImplementedException(
					"Please use method \"verifyCall(Invoked timesInvoked, S mock)\" when using non-strict call verification.");
		}
	}

	public void assertNoMoreMethodInvocations(Object... mocks) {
		for (Object mock : mocks) {
			assert allCalls.get(mock).verifyNoMoreMethodInvocations();
		}
	}

	private void addInterception(Object proxy, MethodCall<Method> methodCall, Object returnValue) {
		Map<MethodCall<Method>, Object> interceptions = allInterceptions.get(proxy);
		if (interceptions == null) {
			interceptions = new HashMap<>();
			InlineMocker.this.allInterceptions.put(proxy, interceptions);
		}
		interceptions.put(methodCall, returnValue);
	}

	public class Stubber<U> {

		private Object proxy;
		private MethodCall<Method> methodCall;

		private Stubber(Object proxy, MethodCall<Method> methodCall) {
			this.proxy = proxy;
			this.methodCall = methodCall;
		}

		public void thenReturn(U returnValue) {
			addInterception(proxy, methodCall, returnValue);
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
		private Object returnValue;

		private AlternativeStubber(Object returnValue) {
			super();
			this.returnValue = returnValue;
		}

		public void whenIntercepted(U dummy) {
		}

		public <V> V when(V mock) {
			return mock;
		}

		private Object getReturnValue() {
			return returnValue;
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

			T calls = allCalls.get(proxy);
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
				calls.registerCall(methodCall.getMethod(), methodCall.getArgs());
				Optional<Object> stubReturnValue = executeStub(proxy, methodCall);
				return stubReturnValue.orElseGet(
						() -> executeDelegate(proxy, methodCall).orElseGet(() -> defaultValueForReturnType(method)));
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
			addInterception(proxy, methodCall, InlineMocker.this.alternativeStubber.getReturnValue());
			return DefaultValues.defaultValue(methodCall.getMethod().getReturnType());
		}

		private Object verifyAndRemoveCall(Object proxy, MethodCall<Method> methodCall, T calls) {
			if (DefaultCalls.class.isAssignableFrom(callsClass)) {
				assert ((DefaultCalls<Method>) calls)
						.verifyAndRemoveCall(InlineMocker.this.verificationState.timesInvoked, methodCall);
			} else if (StrictCalls.class.isAssignableFrom(callsClass)) {
				assert ((StrictCalls<Method>) calls).verifyAndRemoveCall(methodCall);
			} else {
				throw new NotImplementedException(String.format("Calls class %s is unknown and not implemented for %s.",
						callsClass, this.getClass()));
			}
			InlineMocker.this.verificationState.timesInvoked = null;
			return DefaultValues.defaultValue(methodCall.getMethod().getReturnType());
		}

		private Optional<Object> executeStub(Object proxy, MethodCall<Method> methodCall) {
			Map<MethodCall<Method>, Object> interceptions = allInterceptions.get(proxy);
			if (interceptions != null) {
				if (interceptions.containsKey(methodCall)) {
					Object returnValue = interceptions.get(methodCall);
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

		private Object defaultValueForReturnType(Method method) {
			log.info("Method {} was not delegated. Returning default value.", method.getName());
			return DefaultValues.defaultValue(method.getReturnType());
		}
	}

}
