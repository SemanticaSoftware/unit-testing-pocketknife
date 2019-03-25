package com.semantica.pocketknife.calls;

import com.semantica.pocketknife.Mock;

/**
 * The interface that a {@link Mock} should generally reference its
 * {@link Calls} instance with unless strict call verification is required.
 *
 * @author A. Haanstra
 *
 * @param <T>
 */
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

	/**
	 * Convenience method that allows the times a method was invoked to be expressed
	 * more fluently. Otherwise, the same as
	 * {@link #verifyCall(int, Object, Object...)}.
	 *
	 * @param timesInvoked Object expressing the number of times the method is
	 *                     expected to have been called.
	 * @param method       The method identifier
	 * @param args         The arguments, Matchers and/or Predicates with which the
	 *                     method is expected to have been called.
	 * @return True if the given method has been called with the given arguments the
	 *         expected number of times, false otherwise.
	 */
	public boolean verifyCall(Invoked timesInvoked, T method, Object... args);

	/**
	 * Convenience method that allows the times a method was invoked to be expressed
	 * more fluently. Otherwise, the same as {@link #verifyCall(int, MethodCall)}.
	 *
	 * @param timesInvoked Object expressing the number of times the method is
	 *                     expected to have been called.
	 * @param methodCall   The method call identifier
	 * @return True if the given method has been called with the given arguments the
	 *         expected number of times, false otherwise.
	 */
	public boolean verifyCall(Invoked timesInvoked, MethodCall<T> methodCall);

	/**
	 * Convenience method that allows the times a method was invoked to be expressed
	 * more fluently. Otherwise, the same as
	 * {@link #verifyAndRemoveCall(int, Object, Object...)}.
	 *
	 * @param timesInvoked Object expressing the number of times the method is
	 *                     expected to have been called.
	 * @param method       The method identifier
	 * @param args         The arguments, Matchers and/or Predicates with which the
	 *                     method is expected to have been called.
	 * @return True if the given method has been called with the given arguments the
	 *         expected number of times, false otherwise.
	 */
	public boolean verifyAndRemoveCall(Invoked timesInvoked, T method, Object... args);

	/**
	 * Convenience method that allows the times a method was invoked to be expressed
	 * more fluently. Otherwise, the same as
	 * {@link #verifyAndRemoveCall(int, MethodCall)}.
	 *
	 * @param timesInvoked Object expressing the number of times the method is
	 *                     expected to have been called.
	 * @param methodCall   The method call identifier
	 * @return True if the given method has been called with the given arguments the
	 *         expected number of times, false otherwise.
	 */
	public boolean verifyAndRemoveCall(Invoked timesInvoked, MethodCall<T> methodCall);

}
