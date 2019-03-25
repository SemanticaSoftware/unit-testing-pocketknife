package com.semantica.pocketknife.mock;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

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

//	final Map<Class<?>, Map<Method, Object>> newInterceptions = new HashMap<>();
//	final Map<Class<?>, Calls<?>> calls = new HashMap<>();

	/** Intercept a method to return any value */
	final Map<String, Object> interceptions = new HashMap<String, Object>();

	/** Delegation of Interfaces mapping to implementation objects */
	final Map<Class<?>, Object> delegation = new HashMap<Class<?>, Object>();

	public InvocationHandler handler = new CallHandler();

	@SuppressWarnings("unchecked")
	public <T> T mock(Class<T> clazz) {
		return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { clazz }, handler);
	}

	public <T> SimpleMock intercept(String method, T value) {
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

			// Try interceptions first
			if (interceptions.containsKey(method.getName())) {
				return interceptions.get(method.getName());
			}

			// Now try delegations
			Class<?> clientClass = method.getDeclaringClass();
			for (Class<?> type : delegation.keySet()) {
				if (clientClass.isAssignableFrom(type)) {
					try {
						return method.invoke(delegation.get(type), args);
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
