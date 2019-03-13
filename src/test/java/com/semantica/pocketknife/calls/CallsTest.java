package com.semantica.pocketknife.calls;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class CallsTest {

	public void testMethod(Object a) {
	}

	@Test
	public void shouldVerifyAndRemoveCallMatchingExactValue() throws NoSuchMethodException, SecurityException {
		Calls<Method> defaultCalls = CallsFactory.getDefaultCalls();
		Method testMethod = this.getClass().getMethod("testMethod", Object.class);
		Object[] args = { new Object() };
		MethodCall<Method> methodCall = new MethodCall<Method>(testMethod, args);

		defaultCalls.registerCall(testMethod, args);
		assert defaultCalls.verifyNoMoreMethodInvocations() == false;

		defaultCalls.verifyAndRemoveCall(1, methodCall);
		assert defaultCalls.verifyNoMoreMethodInvocations();
	}

	@Test
	public void shouldVerifyAndRemoveCallMatchingMatcher() throws NoSuchMethodException, SecurityException {
		Calls<Method> defaultCalls = CallsFactory.getDefaultCalls();
		Method testMethod = this.getClass().getMethod("testMethod", Object.class);
		Object[] args = { new Object() };
		defaultCalls.registerCall(testMethod, args);

		MethodCall<Method> methodCallWithMatchers = new MethodCall<Method>(testMethod, Matchers.any(Object.class));
		assert defaultCalls.verifyNoMoreMethodInvocations() == false;

		defaultCalls.verifyAndRemoveCall(1, methodCallWithMatchers);
		assert defaultCalls.verifyNoMoreMethodInvocations();
	}

	@Test
	public void shouldVerifyAndRemoveCallMatchingPredicate() throws NoSuchMethodException, SecurityException {
		Calls<Method> defaultCalls = CallsFactory.getDefaultCalls();
		Method testMethod = this.getClass().getMethod("testMethod", Object.class);
		Object[] args = { new Object() };
		defaultCalls.registerCall(testMethod, args);

		MethodCall<Method> methodCallWithMatchers = new MethodCall<Method>(testMethod,
				(Predicate<Object>) object -> object instanceof Object);
		assert defaultCalls.verifyNoMoreMethodInvocations() == false;

		defaultCalls.verifyAndRemoveCall(1, methodCallWithMatchers);
		assert defaultCalls.verifyNoMoreMethodInvocations();
	}

	public Calls<Method> testMethodComplex(boolean a, Boolean b, List<Integer> c, Object d, Object[] e, Object... f) {
		// Normally this Calls instance is declared as class member in a mock.
		Calls<Method> defaultCalls = CallsFactory.getDefaultCalls();

		// This is the typical way in which a mock registers a method call:
		defaultCalls.registerCall(new Object() {
		}.getClass().getEnclosingMethod(), a, b, c, d, e, f);

		return defaultCalls;
	}

	@Test
	public void shouldVerifyAndRemoveComplexCallMatchingExactValue() throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		/**
		 * Testing primitive, wrapper, null, array and varargs (array) values.
		 */
		Method testMethod = this.getClass().getMethod("testMethodComplex", boolean.class, Boolean.class, List.class,
				Object.class, Object[].class, Object[].class);
		Object[] args = { true, false, Arrays.asList(1, 2, 3), null, new Object[] { 1, 2, 3 },
				new Object[] { new Object(), new Object() } };
		MethodCall<Method> methodCall = new MethodCall<Method>(testMethod, args);

		/*
		 * Invoke with reflection to check for compatibility of class MethodCall with
		 * Java Reflection
		 */
		@SuppressWarnings("unchecked")
		Calls<Method> defaultCalls = (Calls<Method>) testMethod.invoke(this, args);
		assert defaultCalls.verifyNoMoreMethodInvocations() == false;

		/*
		 * Assert that we can verify and remove the call using a MethodCall object with
		 * a Java Reflection compatible argument list.
		 */
		defaultCalls.verifyAndRemoveCall(1, methodCall);
		assert defaultCalls.verifyNoMoreMethodInvocations();
	}

	public Calls<Method> testMethodVarargs(Object... a) {
		Calls<Method> defaultCalls = CallsFactory.getDefaultCalls();
		defaultCalls.registerCall(new Object() {
		}.getClass().getEnclosingMethod(), new Object[] { a }); // Prevents expansion into varargs (only necessary for
																// single varargs parameter)
		return defaultCalls;
	}

	@Test
	public void shouldVerifyAndRemoveVarargsCallMatchingExactValue() throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method testMethod = this.getClass().getMethod("testMethodVarargs", Object[].class);
		Object[] args = { new Object[] { new Object(), new Object() } }; // Double array declaration prevents expansion
																			// into varargs
		MethodCall<Method> methodCall = new MethodCall<Method>(testMethod, args);
		@SuppressWarnings("unchecked")
		Calls<Method> defaultCalls = (Calls<Method>) testMethod.invoke(this, args);
		assert defaultCalls.verifyNoMoreMethodInvocations() == false;

		defaultCalls.verifyAndRemoveCall(1, methodCall);
		assert defaultCalls.verifyNoMoreMethodInvocations();
	}

}
