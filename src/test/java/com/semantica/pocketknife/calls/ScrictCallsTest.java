package com.semantica.pocketknife.calls;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

public class ScrictCallsTest {

	private static final boolean NO_STACK_TRACE = false;

	public void testMethodA(Object a) {
	}

	public void testMethodB(Object a) {
	}

	@Test
	public void shouldVerifyAndRemoveCallMatchingExactValue() throws NoSuchMethodException, SecurityException {
		StrictCalls<Method> strictCalls = CallsFactory.getStrictCalls();
		Method testMethod = this.getClass().getMethod("testMethodA", Object.class);
		Object[] args = { new Object() };
		MethodCall<Method> methodCall = new MethodCall<Method>(testMethod, args);

		strictCalls.registerCall(testMethod, args);
		assert strictCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;

		assert strictCalls.verifyAndRemoveCall(methodCall);
		assert strictCalls.verifyNoMoreMethodInvocations();
	}

	@Test
	public void shouldVerifyAndRemoveCallsInSameOrderOfRegistrationIeInvocation()
			throws NoSuchMethodException, SecurityException {
		StrictCalls<Method> strictCalls = CallsFactory.getStrictCalls();
		Method testMethodA = this.getClass().getMethod("testMethodA", Object.class);
		Method testMethodB = this.getClass().getMethod("testMethodB", Object.class);
		Object[] args = { new Object() };
		MethodCall<Method> methodCallA = new MethodCall<Method>(testMethodA, args);
		MethodCall<Method> methodCallB = new MethodCall<Method>(testMethodB, args);

		strictCalls.registerCall(testMethodA, args);
		strictCalls.registerCall(testMethodB, args);

		assert strictCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;
		assert strictCalls.verifyAndRemoveCall(methodCallA);
		assert strictCalls.verifyAndRemoveCall(methodCallB);
		assert strictCalls.verifyNoMoreMethodInvocations();
	}

	@Test
	public void shouldNotVerifyAndRemoveCallsInOrderDifferentFromRegistrationIeInvocation()
			throws NoSuchMethodException, SecurityException {
		StrictCalls<Method> strictCalls = CallsFactory.getStrictCalls();
		Method testMethodA = this.getClass().getMethod("testMethodA", Object.class);
		Method testMethodB = this.getClass().getMethod("testMethodB", Object.class);
		Object[] args = { new Object() };
		MethodCall<Method> methodCallB = new MethodCall<Method>(testMethodB, args);

		strictCalls.registerCall(testMethodA, args);
		strictCalls.registerCall(testMethodB, args);

		assert strictCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;
		assert strictCalls.verifyAndRemoveCall(methodCallB) == false;
	}

	@Test
	public void shouldVerifyAndRemoveDuplicateCallsOneAfterTheOther() throws NoSuchMethodException, SecurityException {
		StrictCalls<Method> strictCalls = CallsFactory.getStrictCalls();
		Method testMethod = this.getClass().getMethod("testMethodA", Object.class);
		Object[] args = { new Object() };
		MethodCall<Method> methodCall = new MethodCall<Method>(testMethod, args);

		strictCalls.registerCall(testMethod, args);// duplicate calls
		strictCalls.registerCall(testMethod, args);
		assert strictCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;

		assert strictCalls.verifyAndRemoveCall(methodCall);
		assert strictCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;
		assert strictCalls.verifyAndRemoveCall(methodCall);
		assert strictCalls.verifyNoMoreMethodInvocations();
	}

	public StrictCalls<String> testMethodWithAlternativeInvocationRegistration(Object a) {
		StrictCalls<String> strictCalls = CallsFactory.getStrictCallsUsingStrings();
		strictCalls.registerCall(a);
		return strictCalls;
	}

	@Test
	public void shouldVerifyAndRemoveCallMatchingExactValueWithAlternativeMethods()
			throws NoSuchMethodException, SecurityException {
		String methodName = "testMethodWithAlternativeInvocationRegistration";
		Object arg = new Object();

		StrictCalls<String> strictCalls = testMethodWithAlternativeInvocationRegistration(arg);
		assert strictCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;

		assert strictCalls.verifyAndRemoveCall(methodName, arg);
		assert strictCalls.verifyNoMoreMethodInvocations();
	}

	@Test
	public void shouldLosePreviousRegistrationAfterReset() throws NoSuchMethodException, SecurityException {
		StrictCalls<Method> strictCalls = CallsFactory.getStrictCalls();
		Method testMethodA = this.getClass().getMethod("testMethodA", Object.class);
		Method testMethodB = this.getClass().getMethod("testMethodB", Object.class);
		Object[] args = { new Object() };
		MethodCall<Method> methodCallA = new MethodCall<Method>(testMethodA, args);
		MethodCall<Method> methodCallB = new MethodCall<Method>(testMethodB, args);

		strictCalls.registerCall(testMethodA, args);
		assert strictCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;
		strictCalls.reset();
		assert strictCalls.verifyNoMoreMethodInvocations();
		strictCalls.registerCall(testMethodB, args);
		assert strictCalls.verifyNoMoreMethodInvocations(NO_STACK_TRACE) == false;

		assert strictCalls.verifyAndRemoveCall(methodCallA) == false;
		assert strictCalls.verifyAndRemoveCall(methodCallB);
		assert strictCalls.verifyNoMoreMethodInvocations();
	}

}
