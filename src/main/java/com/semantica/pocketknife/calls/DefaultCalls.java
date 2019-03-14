package com.semantica.pocketknife.calls;

public interface DefaultCalls<T> extends Calls<T> {

	/**
	 * Verifies that a method has been called a given number of times.
	 *
	 * @param times  Number of times the method is expected to have been called.
	 * @param method The method identifier
	 * @param args   The arguments, Matchers and/or Predicates with which the method
	 *               is expected to have been called.
	 * @return True if the given method has been called with the given arguments the
	 *         expected number of times, false otherwise.
	 */
	public boolean verifyCall(int times, T method, Object... args);

	/**
	 * Convenience method that allows the method call to be specified on one
	 * parameter. Otherwise, the same as
	 * {@link #verifyCall(int, Object, Object...)}.
	 *
	 * @param times      Number of times the method is expected to have been called.
	 *
	 * @param methodCall The method call identifier
	 * @return True if the given method has been called with the given arguments the
	 *         expected number of times, false otherwise.
	 */
	public boolean verifyCall(int times, MethodCall<T> methodCall);

	/**
	 * Verifies that a method has been called a given number of times. If so, this
	 * method will remove the call from the calls registry. This allows checking
	 * that all calls have been verified (then the registry is empty) using
	 * {@link #verifyNoMoreMethodInvocations()}.
	 *
	 * @param times  Number of times the method is expected to have been called.
	 * @param method The method identifier
	 * @param args   The arguments, Matchers and/or Predicates with which the method
	 *               is expected to have been called.
	 * @return True if the given method has been called with the given arguments the
	 *         expected number of times, false otherwise.
	 */
	public boolean verifyAndRemoveCall(int times, T method, Object... args);

	/**
	 * Convenience method that allows the method call to be specified on one
	 * parameter. Otherwise, the same as
	 * {@link #verifyAndRemoveCall(int, Object, Object...)}.
	 *
	 * @param times      Number of times the method is expected to have been called.
	 * @param methodCall The method call identifier
	 * @return True if the given method has been called with the given arguments the
	 *         expected number of times, false otherwise.
	 */
	public boolean verifyAndRemoveCall(int times, MethodCall<T> methodCall);

}
