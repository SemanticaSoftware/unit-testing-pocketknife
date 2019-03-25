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
	final Map<Class<?>, Map<MethodCall<Method>, Object>> allInterceptions = new HashMap<>();
	final Map<Class<?>, Calls<Method>> allCalls = new HashMap<>();

	/** Delegation of Interfaces mapping to implementation objects */
	final Map<Class<?>, Object> delegation = new HashMap<Class<?>, Object>();

	public InvocationHandler handler = new CallHandler();

	@SuppressWarnings("unchecked")
	public <S> S mock(Class<S> clazz, Calls<Method> calls) {
		allCalls.put(clazz, calls);
		return (S) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { clazz }, handler);
	}

	public <S> SimpleMock interceptWith(S returnValue, Method method, Object... args) {
		MethodCall<Method> methodCall = new MethodCall<>(method, args);
		return intercept(methodCall, returnValue);
	}

	public <S> SimpleMock intercept(MethodCall<Method> methodCall, S value) {
		Class<?> declaringClass = methodCall.getMethod().getDeclaringClass();
		Map<MethodCall<Method>, Object> interceptions = allInterceptions.get(declaringClass);
		if (interceptions == null) {
			interceptions = new HashMap<>();
			allInterceptions.put(declaringClass, interceptions);
		}
		interceptions.put(methodCall, value);
		return this;
	}

	public <S> SimpleMock delegate(Class<S> clazz, S value) {
		delegation.put(clazz, value);
		return this;
	}

	private class CallHandler implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (args == null) {
				args = new Object[0];
			}
			Class<?> declaringClass = method.getDeclaringClass();
			MethodCall<Method> methodCall = new MethodCall<>(method, args);

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
			for (Class<?> delegatedClass : delegation.keySet()) {
				List<Method> methodsInDelegatedClass = Arrays.asList(delegatedClass.getMethods());
				if (declaringClass.isAssignableFrom(delegatedClass) && methodsInDelegatedClass.contains(method)) {
					try {
						return method.invoke(delegation.get(delegatedClass), args);
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
