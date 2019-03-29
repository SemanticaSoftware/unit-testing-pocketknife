package com.semantica.pocketknife.util;

import org.opentest4j.AssertionFailedError;

public class Assert {

	private Assert() {
		super();
	}

	public static <T> EqualsAsserter<T> actual(T actual) {
		return new EqualsAsserter<>(actual);
	}

	public static class EqualsAsserter<T> {

		private T actual;

		private EqualsAsserter(T actual) {
			super();
			this.actual = actual;
		}

		public boolean equalsExpected(T expected) {
			boolean equals = expected.equals(actual);
			if (!equals) {
				throw new AssertionFailedError(
						SelectJunitAssertUtils.format(expected, actual, "!actual.equals(expected)"), expected, actual);
			} else {
				return true;
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object expected) {
			return equalsExpected((T) expected);
		}
	}

}
