package com.semantica.pocketknife.calls;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

public class CallsTest {

	public void testMethod(boolean a, Object b) {
	}

	@Test
	public void shouldVerifyAndRemoveRegisteredMethodCall() throws NoSuchMethodException, SecurityException {
		Calls<Method> defaultCalls = CallsFactory.getDefaultCalls();
		Method testMethod = this.getClass().getMethod("testMethod", boolean.class, Object.class);
		Object[] args = { Boolean.TRUE, new Object() };
		MethodCall<Method> methodCall = new MethodCall<Method>(testMethod, args);
		defaultCalls.registerCall(testMethod, args);
		defaultCalls.verifyAndRemoveCall(1, methodCall);
		assert defaultCalls.verifyNoMoreMethodInvocations();
	}

}
