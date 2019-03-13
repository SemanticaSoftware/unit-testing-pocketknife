package com.semantica.pocketknife.calls;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

public class ScrictCallsTest {

	public void testMethod(Object a) {
	}

	@Test
	public void shouldVerifyAndRemoveCallMatchingExactValue() throws NoSuchMethodException, SecurityException {
		Calls<Method> strictCalls = CallsFactory.getStrictCalls();
		Method testMethod = this.getClass().getMethod("testMethod", Object.class);
		Object[] args = { new Object() };
		MethodCall<Method> methodCall = new MethodCall<Method>(testMethod, args);

		strictCalls.registerCall(testMethod, args);
		assert strictCalls.verifyNoMoreMethodInvocations() == false;

		assert strictCalls.verifyStrictlyAndRemoveCall(methodCall);
		assert strictCalls.verifyNoMoreMethodInvocations();
	}

}
