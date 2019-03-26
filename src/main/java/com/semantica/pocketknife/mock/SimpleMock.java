package com.semantica.pocketknife.mock;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.semantica.pocketknife.calls.Calls;
import com.semantica.pocketknife.calls.MethodCall;
import com.semantica.pocketknife.methodrecorder.MethodRecorder;
import com.semantica.pocketknife.util.Tuple;

/**
 * Minimalistic dynamic mock creator class.
 *
 * This class was adapted and modified from
 * https://github.com/brocchini/DIY-mock
 *
 * @author M. Brocchini, A. Haanstra
 * @since 11/2011
 *
 */
public class SimpleMock {

	// mocked class -> intercepted method - > return value
	private final Map<Class<?>, Map<MethodCall<Method>, Object>> allInterceptions = new HashMap<>();
	private final Map<Class<?>, Calls<Method>> allCalls = new HashMap<>();
	private final Map<Object, MethodRecorder<?>> methodRecorders = new HashMap<>();
	private MockBehaviourBuilder<?> initialBehaviourBuilder;

	/** Delegation of Interfaces mapping to implementation objects */
	private final Map<Tuple<Class<?>, ?>, Object> delegation = new HashMap<>();

	private InvocationHandler handler = new CallHandler();

	@SuppressWarnings("unchecked")
	public <S> S mock(Class<S> clazz, Calls<Method> calls) {
		allCalls.put(clazz, calls);
		S proxy = (S) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { clazz }, handler);
		methodRecorders.put(proxy, MethodRecorder.recordInvocationsOn(clazz));
		return proxy;
	}

	public <S> MockBehaviourBuilder<S> whenIntercepted(S dummy) {
		undoCallRegistrationDuringStubbing();
		return initialBehaviourBuilder.typeParameterize();
	}

	private void undoCallRegistrationDuringStubbing() {
		Class<?> declaringClass = initialBehaviourBuilder.getMethodCall().getMethod().getDeclaringClass();
		Calls<Method> calls = allCalls.get(declaringClass);
		calls.removeCall(initialBehaviourBuilder.getMethodCall());
	}

	public <S> AlternativeMockBehaviourBuilder<S> doReturn(S returnValue) {
		return new AlternativeMockBehaviourBuilder<>(returnValue);
	}

	public <S> SimpleMock delegate(Class<S> clazz, S mock, S delegate) {
		Tuple<Class<?>, ?> key = new Tuple<>(clazz, mock);
		delegation.put(key, delegate);
		return this;
	}

	@SuppressWarnings("unchecked")
	public <S> MethodRecorder<S> recorder(S mock) {
		return (MethodRecorder<S>) methodRecorders.get(mock);
	}

	public class MockBehaviourBuilder<U> {

		private Object proxy;
		private MethodCall<Method> methodCall;

		public MockBehaviourBuilder(Object proxy, MethodCall<Method> methodCall) {
			this.proxy = proxy;
			this.methodCall = methodCall;
		}

		public void thenReturn(U returnValue) {
			Class<?> declaringClass = methodCall.getMethod().getDeclaringClass();
			Map<MethodCall<Method>, Object> interceptions = SimpleMock.this.allInterceptions.get(declaringClass);
			if (interceptions == null) {
				interceptions = new HashMap<>();
				SimpleMock.this.allInterceptions.put(declaringClass, interceptions);
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
			undoCallRegistrationDuringStubbing();
			MethodCall<Method> methodCall = SimpleMock.this.initialBehaviourBuilder.getMethodCall();
			Class<?> declaringClass = methodCall.getMethod().getDeclaringClass();
			Map<MethodCall<Method>, Object> interceptions = SimpleMock.this.allInterceptions.get(declaringClass);
			if (interceptions == null) {
				interceptions = new HashMap<>();
				allInterceptions.put(declaringClass, interceptions);
			}
			interceptions.put(methodCall, AlternativeMockBehaviourBuilder.this.returnValue);
		}

	}

	private class CallHandler implements InvocationHandler {
		// TODO: Ensure that call is not registered when setting up mock (seperating
		// phases?). Easier way to verify mock invocations.

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (args == null) {
				args = new Object[0];
			}

			if (method.getName().equals("hashCode") && args.length == 0) {
				return System.identityHashCode(proxy);
			}

			Class<?> declaringClass = method.getDeclaringClass();
			MethodCall<Method> methodCall = new MethodCall<>(method, args);

			// prepare setting up mock behaviour
			initialBehaviourBuilder = new MockBehaviourBuilder<>(proxy, methodCall);
			// Invoke method also on methodRecorder proxy in case we are setting up
			// behaviour
			MethodRecorder<?> methodRecorder = methodRecorders.get(proxy);
			method.invoke(methodRecorder.getProxy(), args);

			// Register calls first
			Calls<Method> calls = allCalls.get(declaringClass);
			calls.registerCall(method, args);

			// Then try interceptions
			Map<MethodCall<Method>, Object> interceptions = allInterceptions.get(declaringClass);
			if (interceptions != null) {
				if (interceptions.containsKey(methodCall)) {
					Object returnValue = interceptions.get(methodCall);
					return returnValue;
				}
			}

			// Now try delegations
			for (Tuple<Class<?>, ?> key : delegation.keySet()) {
				Class<?> clazz = key.getS();
				List<Method> methodsInDelegatedClass = Arrays.asList(clazz.getMethods());
				if (declaringClass.isAssignableFrom(clazz) && proxy == key.getT()
						&& methodsInDelegatedClass.contains(method)) {
					try {
						return method.invoke(delegation.get(key), args);
					} catch (InvocationTargetException e) {
						throw e.getTargetException();
					}
				}
			}

			// Finally return null for non delegated non intercepted methods
			System.out.println(method.getName() + " was not delegated. returning null");
			return null;
		}
	}
}
