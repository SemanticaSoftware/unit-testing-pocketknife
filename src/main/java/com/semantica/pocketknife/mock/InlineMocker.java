package com.semantica.pocketknife.mock;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;

import com.semantica.pocketknife.calls.Calls;
import com.semantica.pocketknife.calls.DefaultCalls;
import com.semantica.pocketknife.calls.Invoked;
import com.semantica.pocketknife.calls.MethodCall;
import com.semantica.pocketknife.calls.StrictCalls;
import com.semantica.pocketknife.methodrecorder.DefaultValues;
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
	// proxy --> ...
	private final Map<Object, Map<MethodCall<Method>, Object>> allInterceptions = new HashMap<>();
	private final Map<Object, T> allCalls = new HashMap<>();
	private MockBehaviourBuilder<?> initialBehaviourBuilder;

	/** Delegation of Interfaces mapping to implementation objects */
	private final Map<Tuple<Class<?>, ?>, Object> delegates = new HashMap<>();

	private PreparedProxyState preparedProxyState = PreparedProxyState.Execution;
	private Object returnValue;
	private Invoked timesInvoked = null;

	private InvocationHandler handler = new CallHandler();

	private enum PreparedProxyState {
		Stubbing, Verification, Execution;
	}

	public InlineMocker(Class<T> callsClass) {
		this.callsClass = callsClass;
	}

	@SuppressWarnings("unchecked")
	public <S> S mock(Class<S> clazz, T calls) {
		S proxy = (S) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { clazz }, handler);
		allCalls.put(proxy, calls);
		return proxy;
	}

	public <S> MockBehaviourBuilder<S> whenIntercepted(S dummy) {
		undoCallRegistrationDuringUnpreparedStubbing();
		return initialBehaviourBuilder.typeParameterize();
	}

	private void undoCallRegistrationDuringUnpreparedStubbing() {
		Calls<Method> calls = allCalls.get(initialBehaviourBuilder.getProxy());
		calls.removeCall(initialBehaviourBuilder.getMethodCall());
	}

	public <S> AlternativeMockBehaviourBuilder<S> doReturn(S returnValue) {
		this.returnValue = returnValue;
		return new AlternativeMockBehaviourBuilder<>(returnValue);
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
			this.timesInvoked = timesInvoked;
			InlineMocker.this.preparedProxyState = PreparedProxyState.Verification;
			return mock;
		} else {
			throw new NotImplementedException(
					"Please use method \"verifyCall(S mock)\" when using strict call verification.");
		}
	}

	public <S> S assertCalled(S mock) {
		if (callsClass == StrictCalls.class) {
			InlineMocker.this.preparedProxyState = PreparedProxyState.Verification;
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

	public class MockBehaviourBuilder<U> {

		private Object proxy;
		private MethodCall<Method> methodCall;

		public MockBehaviourBuilder(Object proxy, MethodCall<Method> methodCall) {
			this.proxy = proxy;
			this.methodCall = methodCall;
		}

		public void thenReturn(U returnValue) {
			Map<MethodCall<Method>, Object> interceptions = InlineMocker.this.allInterceptions
					.get(MockBehaviourBuilder.this.proxy);
			if (interceptions == null) {
				interceptions = new HashMap<>();
				InlineMocker.this.allInterceptions.put(MockBehaviourBuilder.this.proxy, interceptions);
			}
			interceptions.put(methodCall, returnValue);
		}

		public <V> MockBehaviourBuilder<V> typeParameterize() {
			return new MockBehaviourBuilder<>(proxy, methodCall);
		}

		public Object getProxy() {
			return proxy;
		}

		public MethodCall<Method> getMethodCall() {
			return methodCall;
		}
	}

	public class AlternativeMockBehaviourBuilder<U> {
		private Object returnValue;

		public AlternativeMockBehaviourBuilder(Object returnValue) {
			super();
			this.returnValue = returnValue;
		}

		public void whenIntercepted(U dummy) {
			undoCallRegistrationDuringUnpreparedStubbing();
			MethodCall<Method> methodCall = InlineMocker.this.initialBehaviourBuilder.getMethodCall();
			Object proxy = InlineMocker.this.initialBehaviourBuilder.getProxy();
			Map<MethodCall<Method>, Object> interceptions = InlineMocker.this.allInterceptions.get(proxy);
			if (interceptions == null) {
				interceptions = new HashMap<>();
				allInterceptions.put(proxy, interceptions);
			}
			interceptions.put(methodCall, AlternativeMockBehaviourBuilder.this.returnValue);
		}

		public <V> V when(V mock) {
			InlineMocker.this.preparedProxyState = PreparedProxyState.Stubbing;
			return mock;
		}

	}

	private class CallHandler implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (args == null) {
				args = new Object[0];
			}
			if (method.getName().equals("hashCode") && args.length == 0) {
				return System.identityHashCode(proxy);
			} else if (method.getName().equals("toString") && args.length == 0) {
				return "Mock proxy object with hashCode: " + System.identityHashCode(proxy);
			} else if (method.getName().equals("equals") && args.length == 1) {
				return System.identityHashCode(proxy) == System.identityHashCode(args[0]);
			}

			MethodCall<Method> methodCall = new MethodCall<>(method, args);
			T calls = allCalls.get(proxy);

			if (InlineMocker.this.preparedProxyState == PreparedProxyState.Stubbing) {
				// doReturn(retVal).when(mock).someMethod()
				Map<MethodCall<Method>, Object> interceptions = InlineMocker.this.allInterceptions.get(proxy);
				if (interceptions == null) {
					interceptions = new HashMap<>();
					allInterceptions.put(proxy, interceptions);
				}
				interceptions.put(methodCall, InlineMocker.this.returnValue);
				InlineMocker.this.preparedProxyState = PreparedProxyState.Execution;
				return DefaultValues.defaultValue(method.getReturnType());
			} else if (InlineMocker.this.preparedProxyState == PreparedProxyState.Verification) {
				if (DefaultCalls.class.isAssignableFrom(callsClass)) {
					assert ((DefaultCalls<Method>) calls).verifyAndRemoveCall(timesInvoked, methodCall);
				} else if (StrictCalls.class.isAssignableFrom(callsClass)) {
					assert ((StrictCalls<Method>) calls).verifyAndRemoveCall(methodCall);
				} else {
					throw new NotImplementedException(String.format(
							"Calls class %s is unknown and not implemented for %s.", callsClass, this.getClass()));
				}
				InlineMocker.this.preparedProxyState = PreparedProxyState.Execution;
				timesInvoked = null;
				return DefaultValues.defaultValue(method.getReturnType());
			} else {// PreparedProxyState.Execution
				// Prepare setting up mock behaviour in case we are doing an unprepared stubbing
				initialBehaviourBuilder = new MockBehaviourBuilder<>(proxy, methodCall);
				// Register this proxy method invocation for later verification (needs to be
				// removed for unprepared stubbing)
				calls.registerCall(method, args);
				// Interceptions go before delegations
				Map<MethodCall<Method>, Object> interceptions = allInterceptions.get(proxy);
				if (interceptions != null) {
					if (interceptions.containsKey(methodCall)) {
						Object returnValue = interceptions.get(methodCall);
						return returnValue;
					}
				}
				/*
				 * Both the mock and delegate are instances of the interface (see delegate(..)
				 * method). We only want to delegate methods in the interface. If the current
				 * method is in the interface, the declaring class is the interface. Therefore,
				 * we request a tuple key with this interface and proxy, and invoke the delegate
				 * if it has been set (not null).
				 *
				 */
				Object delegate = delegates.get(new Tuple<>(method.getDeclaringClass(), proxy));
				if (delegate != null) {
					try {
						return method.invoke(delegate, args);
					} catch (InvocationTargetException e) {
						log.error("Cannot invoke delegate method {}.", method, e);
						throw e.getTargetException();
					}
				}
				log.info("Method {} was not delegated. Returning default value.", method.getName());
				return DefaultValues.defaultValue(method.getReturnType());
			}
		}
	}
}
