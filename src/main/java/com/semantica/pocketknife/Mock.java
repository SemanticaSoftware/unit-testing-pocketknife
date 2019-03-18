package com.semantica.pocketknife;

import com.semantica.pocketknife.calls.Calls;

/**
 * Each mock should implement this interface to ensure that (1) Method calls can
 * be verified on this mock [getCalls()]. (2) The mock instance can be reused
 * over several tests [reset()].
 *
 * @author A. Haanstra
 *
 */
public interface Mock {

	/**
	 * Returns a Calls instance to facilitate verification of method calls on this
	 * mock.
	 *
	 * @return This Mock's Calls instance
	 */
	public Calls<?> getCalls();

	/**
	 * Resets all state to initial values for this mock.
	 */
	public void reset();

}
