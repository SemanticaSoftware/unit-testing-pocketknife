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
	final Map<Class<?>, Map<Method, Object>> allInterceptions = new HashMap<>();
	final Map<Class<?>, Calls<Method>> allCalls = new HashMap<>();

	/** Intercept a method to return any value */
	final Map<String, Object> interceptions = new HashMap<String, Object>();

	/** Delegation of Interfaces mapping to implementation objects */
	final Map<Class<?>, Object> delegation = new HashMap<Class<?>, Object>();

	public InvocationHandler handler = new CallHandler();

	@SuppressWarnings("unchecked")
	public <T> T mock(Class<T> clazz, Calls<Method> calls) {
		allCalls.put(clazz, calls);
		return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { clazz }, handler);
	}

	public <T> SimpleMock intercept(Method method, T value) {
		Class<?> declaringClass = method.getDeclaringClass();
		Map<Method, Object> interceptions = allInterceptions.get(declaringClass);
		if (interceptions == null) {
			interceptions = new HashMap<>();
			allInterceptions.put(declaringClass, interceptions);
		}
		interceptions.put(method, value);
		return this;
	}

	public <T> SimpleMock delegate(Class<T> clazz, T value) {
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

			// Register calls first
			Calls<Method> calls = allCalls.get(declaringClass);
			calls.registerCall(method, args);

			// Then try interceptions
			Map<Method, Object> interceptions = allInterceptions.get(declaringClass);
			if (interceptions != null) {
				if (interceptions.containsKey(method)) {
					Object returnValue = interceptions.get(method);
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
