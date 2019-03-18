package com.semantica.pocketknife.calls;

import java.lang.reflect.Method;

import org.apache.commons.lang3.NotImplementedException;

/**
 * Factory class providing an easy and abstracted way to construct a
 * {@link Calls} instance.
 *
 * @author A. Haanstra
 *
 */
public class CallsFactory {

	public enum CallType {
		Strict, Default;
	}

	/**
	 * Factory method for creating a default Calls object that uses method name
	 * Strings and an Object[] of method arguments internally to identify methods.
	 *
	 * @return A Calls object that uses default mock verification. Method calls can
	 *         be registered simply only passing the method arguments.
	 */
	public static DefaultCalls<String> getDefaultCallsUsingStrings() {
		return (DefaultCalls<String>) getCalls(CallType.Default, String.class);
	}

	/**
	 * Factory method for creating a strictly verifying Calls object that uses
	 * method name Strings and an Object[] of method arguments internally to
	 * identify methods.
	 *
	 * @return A Calls object that uses strict mock verification. Method calls can
	 *         be registered simply only passing the method arguments.
	 */
	public static StrictCalls<String> getStrictCallsUsingStrings() {
		return (StrictCalls<String>) getCalls(CallType.Strict, String.class);
	}

	/**
	 * Factory method for creating a default Calls object that uses Method instances
	 * for identifying method names and an Object[] of method arguments internally
	 * to identify methods.
	 *
	 * @return A Calls object that uses default mock verification. Method calls need
	 *         to be registered by passing both a java.lang.reflect.Method and an
	 *         array of arguments.
	 */
	public static DefaultCalls<Method> getDefaultCalls() {
		return (DefaultCalls<Method>) getCalls(CallType.Default, Method.class);
	}

	/**
	 * Factory method for creating a strictly verifying Calls object that uses
	 * Method instances for identifying method names and an Object[] of method
	 * arguments internally to identify methods.
	 *
	 * @return A Calls object that uses strict mock verification. Method calls need
	 *         to be registered by passing both a java.lang.reflect.Method and an
	 *         array of arguments.
	 */
	public static StrictCalls<Method> getStrictCalls() {
		return (StrictCalls<Method>) getCalls(CallType.Strict, Method.class);
	}

	/**
	 * Factory method that allows its user to completely specify the type of Calls
	 * object returned.
	 *
	 * @param callType              one of {@link CallType#Strict} or
	 *                              {@link CallType#Default}
	 * @param methodIdentifierClass The class to identify method parameters with.
	 *                              Can be either {@link String} or {@link Method}.
	 *                              In principle, any class might be used but is not
	 *                              recommended.
	 * @return A default or strictly verifying Calls object parameterized with the
	 *         same type as {@code methodIdentifierClass}.
	 */
	public static <T> Calls<T> getCalls(CallType callType, Class<T> methodIdentifierClass) {
		switch (callType) {
		case Strict:
			return new StrictCallsRegistry<>(methodIdentifierClass);
		case Default:
			return new CallsRegistry<>(methodIdentifierClass);
		default:
			throw new NotImplementedException("Unknown " + CallType.class.getSimpleName());
		}
	}

}
