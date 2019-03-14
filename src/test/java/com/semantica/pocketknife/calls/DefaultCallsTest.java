package com.semantica.pocketknife.calls;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class DefaultCallsTest {

	private static final boolean NO_STACK_TRACE = false;

	public void testMethod(Object a) {
	}

	@Test
	public void shouldVerifyAndRemoveCallMatchingExactValue() throws NoSuchMethodException, SecurityException {
		DefaultCalls<Method> defaultCalls = CallsFactory.getDefaultCalls();
		Method testMethod = this.getClass().getMethod("testMethod", Object.class);
		Object[] args = { new Object() };
		MethodCall<Method> methodCall = new MethodCall<Method>(testMethod, args);

		defaultCalls.registerCall(testMethod, args);
		assert defaultCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;

		assert defaultCalls.verifyCall(1, methodCall);
		assert defaultCalls.verifyAndRemoveCall(1, methodCall);
		assert defaultCalls.verifyNoMoreMethodInvocations();
	}

	@Test
	public void shouldVerifyAndRemoveCallMatchingMatcher() throws NoSuchMethodException, SecurityException {
		DefaultCalls<Method> defaultCalls = CallsFactory.getDefaultCalls();
		Method testMethod = this.getClass().getMethod("testMethod", Object.class);
		Object[] args = { new Object() };
		defaultCalls.registerCall(testMethod, args);

		MethodCall<Method> methodCallWithMatchers = new MethodCall<Method>(testMethod, Matchers.any(Object.class));
		assert defaultCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;

		assert defaultCalls.verifyCall(1, methodCallWithMatchers);
		assert defaultCalls.verifyAndRemoveCall(1, methodCallWithMatchers);
		assert defaultCalls.verifyNoMoreMethodInvocations();
	}

	@Test
	public void shouldVerifyAndRemoveCallMatchingPredicate() throws NoSuchMethodException, SecurityException {
		DefaultCalls<Method> defaultCalls = CallsFactory.getDefaultCalls();
		Method testMethod = this.getClass().getMethod("testMethod", Object.class);
		Object[] args = { new Object() };
		defaultCalls.registerCall(testMethod, args);

		MethodCall<Method> methodCallWithMatchers = new MethodCall<Method>(testMethod,
				(Predicate<Object>) object -> object instanceof Object);
		assert defaultCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;

		assert defaultCalls.verifyCall(1, methodCallWithMatchers);
		assert defaultCalls.verifyAndRemoveCall(1, methodCallWithMatchers);
		assert defaultCalls.verifyNoMoreMethodInvocations();
	}

	public Calls<Method> testMethodComplex(boolean a, Boolean b, List<Integer> c, Object d, Object[] e, Object... f) {
		// Normally this Calls instance is declared as class member in a mock.
		DefaultCalls<Method> defaultCalls = CallsFactory.getDefaultCalls();

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
		DefaultCalls<Method> defaultCalls = (DefaultCalls<Method>) testMethod.invoke(this, args);
		assert defaultCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;

		/*
		 * Assert that we can verify and remove the call using a MethodCall object with
		 * a Java Reflection compatible argument list.
		 */
		assert defaultCalls.verifyCall(1, methodCall);
		assert defaultCalls.verifyAndRemoveCall(1, methodCall);
		assert defaultCalls.verifyNoMoreMethodInvocations();
	}

	@Test
	public void shouldVerifyAndRemoveComplexCallMatchingWithExactValuesMatchersAndPredicates()
			throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		/**
		 * Testing primitive, wrapper, null, array and varargs (array) values.
		 */
		Method testMethod = this.getClass().getMethod("testMethodComplex", boolean.class, Boolean.class, List.class,
				Object.class, Object[].class, Object[].class);
		Object[] objectArray = { new Object(), new Object() };
		Object[] matchingArgs = { (Predicate<Boolean>) bool -> bool, false, Matchers.hasSize(3), Matchers.anything(),
				(Predicate<Object[]>) array -> array.length == 3, objectArray };
		MethodCall<Method> methodCall = new MethodCall<Method>(testMethod, matchingArgs);

		/*
		 * Invoke with reflection to check for compatibility of class MethodCall with
		 * Java Reflection
		 */
		Object[] args = { true, false, Arrays.asList(1, 2, 3), null, new Object[] { 1, 2, 3 }, objectArray };
		@SuppressWarnings("unchecked")
		DefaultCalls<Method> defaultCalls = (DefaultCalls<Method>) testMethod.invoke(this, args);
		assert defaultCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;

		/*
		 * Assert that we can verify and remove the call using a MethodCall object with
		 * a Java Reflection compatible argument list.
		 */
		assert defaultCalls.verifyCall(1, methodCall);
		assert defaultCalls.verifyAndRemoveCall(1, methodCall);
		assert defaultCalls.verifyNoMoreMethodInvocations();
	}

	public DefaultCalls<Method> testMethodVarargs(Object... a) {
		DefaultCalls<Method> defaultCalls = CallsFactory.getDefaultCalls();
		defaultCalls.registerCall(new Object() {
		}.getClass().getEnclosingMethod(), new Object[] { a }); // Prevents expansion into varargs (only necessary for
																// single varargs parameter: compare with
																// testMethodComplex(...))
		return defaultCalls;
	}

	@Test
	public void shouldVerifyAndRemoveVarargsCallMatchingExactValue() throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method testMethod = this.getClass().getMethod("testMethodVarargs", Object[].class);
		Object[] args = { new Object[] { new Object(), new Object() } }; // Double array creation prevents expansion
																			// into varargs
		MethodCall<Method> methodCall = new MethodCall<Method>(testMethod, args);
		@SuppressWarnings("unchecked")
		DefaultCalls<Method> defaultCalls = (DefaultCalls<Method>) testMethod.invoke(this, args);
		assert defaultCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;

		assert defaultCalls.verifyCall(1, methodCall);
		assert defaultCalls.verifyAndRemoveCall(1, methodCall);
		assert defaultCalls.verifyNoMoreMethodInvocations();
	}

	public DefaultCalls<String> testMethodWithAlternativeInvocationRegistration(Object a) {
		DefaultCalls<String> defaultCalls = CallsFactory.getDefaultCallsUsingStrings();
		defaultCalls.registerCall(a);
		return defaultCalls;
	}

	@Test
	public void shouldVerifyAndRemoveCallMatchingExactValueWithAlternativeMethods()
			throws NoSuchMethodException, SecurityException {
		String methodName = "testMethodWithAlternativeInvocationRegistration";
		Object arg = new Object();

		DefaultCalls<String> defaultCalls = testMethodWithAlternativeInvocationRegistration(arg);
		assert defaultCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;

		assert defaultCalls.verifyCall(1, methodName, arg);
		assert defaultCalls.verifyAndRemoveCall(1, methodName, arg);
		assert defaultCalls.verifyNoMoreMethodInvocations();
	}

	public void otherTestMethod(Object a) {
	}

	@Test
	public void shouldNotVerifyAndRemoveCallNotMatchingOnMethodName() throws NoSuchMethodException, SecurityException {
		DefaultCalls<Method> defaultCalls = CallsFactory.getDefaultCalls();
		Method registeredTestMethod = this.getClass().getMethod("testMethod", Object.class);
		Object[] registeredArgs = { new Object() };
		MethodCall<Method> methodCall = new MethodCall<Method>(registeredTestMethod, registeredArgs);

		defaultCalls.registerCall(methodCall);
		assert defaultCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;

		Method otherTestMethod = this.getClass().getMethod("otherTestMethod", Object.class);
		MethodCall<Method> otherMethodCall = new MethodCall<Method>(otherTestMethod, registeredArgs);
		assert defaultCalls.verifyCall(1, otherMethodCall) == false;
		assert defaultCalls.verifyAndRemoveCall(1, otherMethodCall) == false;
		assert defaultCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;
	}

	@Test
	public void shouldNotVerifyAndRemoveCallNotMatchingOnArgs() throws NoSuchMethodException, SecurityException {
		DefaultCalls<Method> defaultCalls = CallsFactory.getDefaultCalls();
		Method registeredTestMethod = this.getClass().getMethod("testMethod", Object.class);
		Object[] registeredArgs = { new Object() };
		MethodCall<Method> methodCall = new MethodCall<Method>(registeredTestMethod, registeredArgs);

		defaultCalls.registerCall(methodCall);
		assert defaultCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;

		Object[] otherArgs = { new Object() }; // i.e. other instance
		MethodCall<Method> otherMethodCall = new MethodCall<Method>(registeredTestMethod, otherArgs);
		assert defaultCalls.verifyCall(1, otherMethodCall) == false;
		assert defaultCalls.verifyAndRemoveCall(1, otherMethodCall) == false;
		assert defaultCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;
	}

	@Test
	public void shouldNotVerifyAndRemoveCallNotMatchingOnTimesInvoked()
			throws NoSuchMethodException, SecurityException {
		DefaultCalls<Method> defaultCalls = CallsFactory.getDefaultCalls();
		Method registeredTestMethod = this.getClass().getMethod("testMethod", Object.class);
		Object[] registeredArgs = { new Object() };
		MethodCall<Method> methodCall = new MethodCall<Method>(registeredTestMethod, registeredArgs);

		defaultCalls.registerCall(methodCall);
		assert defaultCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;

		assert defaultCalls.verifyCall(0, methodCall) == false;
		assert defaultCalls.verifyCall(-1, methodCall) == false;
		assert defaultCalls.verifyAndRemoveCall(2, methodCall) == false;
		assert defaultCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;
	}

	@Test
	public void shouldVerifyThatMethodNeverInvokedForImaginaryCall() throws NoSuchMethodException, SecurityException {
		DefaultCalls<Method> defaultCalls = CallsFactory.getDefaultCalls();
		Method neverInvokedTestMethod = this.getClass().getMethod("testMethod", Object.class);
		Object[] neverUsedArgs = { new Object() };
		MethodCall<Method> imaginaryMethodCall = new MethodCall<Method>(neverInvokedTestMethod, neverUsedArgs);

		assert defaultCalls.verifyNoMoreMethodInvocations();
		assert defaultCalls.verifyCall(0, imaginaryMethodCall);
		assert defaultCalls.verifyAndRemoveCall(0, imaginaryMethodCall);
	}

	@Test
	public void shouldVerifyCallBeforeButNotAfterReset() throws NoSuchMethodException, SecurityException {
		DefaultCalls<Method> defaultCalls = CallsFactory.getDefaultCalls();
		Method testMethod = this.getClass().getMethod("testMethod", Object.class);
		Object[] args = { new Object() };
		MethodCall<Method> methodCall = new MethodCall<Method>(testMethod, args);

		defaultCalls.registerCall(testMethod, args);
		assert defaultCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;
		assert defaultCalls.verifyCall(1, methodCall);

		defaultCalls.reset();
		assert defaultCalls.verifyCall(0, methodCall);
		assert defaultCalls.verifyNoMoreMethodInvocations();
	}

}
