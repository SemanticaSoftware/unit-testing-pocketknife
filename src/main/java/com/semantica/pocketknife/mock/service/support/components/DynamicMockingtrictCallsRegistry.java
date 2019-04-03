package com.semantica.pocketknife.mock.service.support.components;

import com.semantica.pocketknife.calls.MethodCall;
import com.semantica.pocketknife.calls.StrictCallsRegistry;
import com.semantica.pocketknife.util.TestUtils;

public class DynamicMockingtrictCallsRegistry<T> extends StrictCallsRegistry<T> {

	public DynamicMockingtrictCallsRegistry(Class<T> methodClass) {
		super(methodClass);
	}

	/*
	 * The normal use of a calls registry is inside a manually defined mock. Then a
	 * call is registered right in the called method itself. For an inline mock, it
	 * is registered from the proxy's intercept method. Therefore it is more useful
	 * to show the stack trace from the point where the method was invoked on the
	 * mock. This is a distance of 1 method further away than usual
	 * com.semantica.pocketknife.mock.InlineMocker$CallHandler.invoke(..) <-- here
	 * call is registered com.sun.proxy.$Proxy24.someMethod(..) <-- extra method for
	 * inline mocks MyTest.myTestMethod(..)<-- here method is invoked on mock
	 *
	 * Since there is an extra method and we rather don't want to show the
	 * stacktrace from the point where the call is registered (in the library), we
	 * use a distance of 2 methods.
	 */
	@Override
	public void registerCall(T method, Object... args) {
		requireNonNull(args);
		MethodCall<T> methodCall = new MethodCall<>(method, args);
		addStackTraceToCalls(methodCall, TestUtils.getTruncatedStackTrace(2));
	}

	@Override
	public void registerCall(MethodCall<T> methodCall) {
		requireNonNull(methodCall.getArgs());
		addStackTraceToCalls(methodCall, TestUtils.getTruncatedStackTrace(2));
	}

}
