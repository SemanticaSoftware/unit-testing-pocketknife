package com.semantica.pocketknife.util;

public class TestUtils {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestUtils.class);

	/**
	 * Get the method name for a {@code depth} in call stack.
	 *
	 * For depth=0, this method returns the method name of the current method (from
	 * where this method {@link #traceLogMethodCall(int)} was invoked) and the
	 * method from which the invoking method was called.
	 *
	 * @param depth The {@code depth} in the call stack (0 means current method, 1
	 *              means invoking method, ...)
	 * @return The method name at the requested {@code depth}.
	 */
	public static String getMethodName(final int depth) {
		final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		return ste[2 + depth].getMethodName();
	}

	/**
	 * Log the method name for a {@code depth} in call stack.
	 *
	 * For depth=0, this method logs the method name of the current method (from
	 * where this method {@link #traceLogMethodCall(int)} was invoked) and the
	 * method from which the invoking method was called.
	 *
	 * @param depth The {@code depth} in the call stack (0 means current method, 1
	 *              means invoking method, ...)
	 */
	public static void traceLogMethodCall(final int depth) {
		log.trace("In method: " + Thread.currentThread().getStackTrace()[2 + depth] + ", called from: "
				+ Thread.currentThread().getStackTrace()[3 + depth]);
	}

	/**
	 * Returns the corresponding english ordinal number for the given cardinal
	 * number (e.g. 1st for 1).
	 *
	 * @param cardinal The number for which an ordinal number is requested
	 * @return The corresponding ordinal number in informal notation (e.g. "1st" but
	 *         not "first").
	 */
	public static String getOrdinal(int cardinal) {
		if (cardinal % 10 == 1 && cardinal != 11) {
			return cardinal + "st";
		} else if (cardinal % 10 == 2 && cardinal != 12) {
			return cardinal + "nd";
		} else if (cardinal % 10 == 3 && cardinal != 13) {
			return cardinal + "rd";
		} else {
			return cardinal + "th";
		}
	}

}
