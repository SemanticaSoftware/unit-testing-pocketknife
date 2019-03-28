package com.semantica.pocketknife.util;

import org.opentest4j.AssertionFailedError;

public class Assert {

	private Assert() {
		super();
	}

	public static class Asserter<T> {

		private T actual;

		private Asserter(T actual) {
			super();
			this.actual = actual;
		}

		public void equalsExpected(T expected) {
			if (!actual.equals(expected)) {
				throw new AssertionFailedError(
						SelectJunitAssertUtils.format(expected, actual, "!actual.equals(expected)"), expected, actual);
			}
		}
	}

	public static <T> Asserter<T> actual(T actual) {
		return new Asserter<>(actual);
	}

}
