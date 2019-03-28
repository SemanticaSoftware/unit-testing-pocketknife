package com.semantica.pocketknife.util;

import java.util.Arrays;

/* These utility method have been copied from the JUnit project (JUnit Jupiter API).
 *
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */
public class SelectJunitAssertUtils {

	static String format(Object expected, Object actual, String message) {
		return buildPrefix(message) + formatValues(expected, actual);
	}

	static String buildPrefix(String message) {
		return (isNotBlank(message) ? message + " ==> " : "");
	}

	/**
	 * Determine if the supplied {@link String} is not {@linkplain #isBlank blank}.
	 *
	 * @param str the string to check; may be {@code null}
	 * @return {@code true} if the string is not blank
	 * @see #isBlank(String)
	 */
	public static boolean isNotBlank(String str) {
		return !isBlank(str);
	}

	/**
	 * Determine if the supplied {@link String} is <em>blank</em> (i.e.,
	 * {@code null} or consisting only of whitespace characters).
	 *
	 * @param str the string to check; may be {@code null}
	 * @return {@code true} if the string is blank
	 * @see #isNotBlank(String)
	 */
	public static boolean isBlank(String str) {
		return (str == null || str.trim().isEmpty());
	}

	static String formatValues(Object expected, Object actual) {
		String expectedString = toString(expected);
		String actualString = toString(actual);
		if (expectedString.equals(actualString)) {
			return String.format("expected: %s but was: %s", formatClassAndValue(expected, expectedString),
					formatClassAndValue(actual, actualString));
		}
		return String.format("expected: <%s> but was: <%s>", expectedString, actualString);
	}

	private static String toString(Object obj) {
		if (obj instanceof Class) {
			return getCanonicalName((Class<?>) obj);
		}
		return nullSafeToString(obj);
	}

	private static String formatClassAndValue(Object value, String valueString) {
		String classAndHash = getClassName(value) + toHash(value);
		// if it's a class, there's no need to repeat the class name contained in the
		// valueString.
		return (value instanceof Class ? "<" + classAndHash + ">" : classAndHash + "<" + valueString + ">");
	}

	private static String getClassName(Object obj) {
		return (obj == null ? "null"
				: obj instanceof Class ? getCanonicalName((Class<?>) obj) : obj.getClass().getName());
	}

	private static String toHash(Object obj) {
		return (obj == null ? "" : "@" + Integer.toHexString(System.identityHashCode(obj)));
	}

	static String getCanonicalName(Class<?> clazz) {
		try {
			String canonicalName = clazz.getCanonicalName();
			return (canonicalName != null ? canonicalName : clazz.getName());
		} catch (Throwable t) {
			rethrowIfBlacklisted(t);
			return clazz.getName();
		}
	}

	private static void rethrowIfBlacklisted(Throwable t) {
		if (t instanceof OutOfMemoryError) {
			throw (OutOfMemoryError) t;
		}
	}

	/**
	 * Convert the supplied {@code Object} to a {@code String} using the following
	 * algorithm.
	 *
	 * <ul>
	 * <li>If the supplied object is {@code null}, this method returns
	 * {@code "null"}.</li>
	 * <li>If the supplied object is a primitive array, the appropriate
	 * {@code Arrays#toString(...)} variant will be used to convert it to a
	 * String.</li>
	 * <li>If the supplied object is an object array,
	 * {@code Arrays#deepToString(Object[])} will be used to convert it to a
	 * String.</li>
	 * <li>Otherwise, the result of invoking {@code toString()} on the object will
	 * be returned.</li>
	 * <li>If any of the above results in an exception, this method delegates to
	 * {@link #defaultToString(Object)}</li>
	 * </ul>
	 *
	 * @param obj the object to convert to a String; may be {@code null}
	 * @return a String representation of the supplied object; never {@code null}
	 * @see Arrays#deepToString(Object[])
	 * @see ClassUtils#nullSafeToString(Class...)
	 */
	public static String nullSafeToString(Object obj) {
		if (obj == null) {
			return "null";
		}

		try {
			if (obj.getClass().isArray()) {
				if (obj.getClass().getComponentType().isPrimitive()) {
					if (obj instanceof boolean[]) {
						return Arrays.toString((boolean[]) obj);
					}
					if (obj instanceof char[]) {
						return Arrays.toString((char[]) obj);
					}
					if (obj instanceof short[]) {
						return Arrays.toString((short[]) obj);
					}
					if (obj instanceof byte[]) {
						return Arrays.toString((byte[]) obj);
					}
					if (obj instanceof int[]) {
						return Arrays.toString((int[]) obj);
					}
					if (obj instanceof long[]) {
						return Arrays.toString((long[]) obj);
					}
					if (obj instanceof float[]) {
						return Arrays.toString((float[]) obj);
					}
					if (obj instanceof double[]) {
						return Arrays.toString((double[]) obj);
					}
				}
				return Arrays.deepToString((Object[]) obj);
			}

			// else
			return obj.toString();
		} catch (Throwable throwable) {
			rethrowIfBlacklisted(throwable);
			return defaultToString(obj);
		}
	}

	/**
	 * Convert the supplied {@code Object} to a <em>default</em> {@code String}
	 * representation using the following algorithm.
	 *
	 * <ul>
	 * <li>If the supplied object is {@code null}, this method returns
	 * {@code "null"}.</li>
	 * <li>Otherwise, the String returned by this method will be generated analogous
	 * to the default implementation of {@link Object#toString()} by using the
	 * supplied object's class name and hash code as follows:
	 * {@code obj.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(obj))}</li>
	 * </ul>
	 *
	 * @param obj the object to convert to a String; may be {@code null}
	 * @return the default String representation of the supplied object; never
	 *         {@code null}
	 * @see #nullSafeToString(Object)
	 * @see ClassUtils#nullSafeToString(Class...)
	 */
	public static String defaultToString(Object obj) {
		if (obj == null) {
			return "null";
		}
		return obj.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(obj));
	}

}
