package com.semantica.pocketknife.calls;

public interface StrictCalls<T> extends Calls<T> {

	/**
	 * Verifies that a method has been called using strict verification. Method
	 * calls need to be verified in exactly the same order as they were registered
	 * by the mock. If the call was successfully verified, this method will remove
	 * the call from the calls registry. This allows checking that all calls have
	 * been verified (then the registry is empty) using
	 * {@link #verifyNoMoreMethodInvocations()}.
	 *
	 * @param method The method identifier
	 * @param args   The arguments, Matchers and/or Predicates with which the method
	 *               is expected to have been called.
	 * @return True if the given method has been called with the given arguments the
	 *         expected number of times, false otherwise.
	 */
	public boolean verifyAndRemoveCall(T method, Object... args);

	/**
	 * Convenience method that allows the method call to be specified on one
	 * parameter. Otherwise, the same as
	 * {@link #verifyAndRemoveCall(Object, Object...)}.
	 *
	 * @param methodCall The method call identifier
	 * @return True if the given method has been called with the given arguments the
	 *         expected number of times, false otherwise.
	 */
	public boolean verifyAndRemoveCall(MethodCall<T> methodCall);

}
