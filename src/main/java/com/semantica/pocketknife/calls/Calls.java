package com.semantica.pocketknife.calls;

public interface Calls<T> {

	/**
	 * Register a method call. The method name is inferred from the stack trace.
	 * Only available when T is {@link String}, i.e. when {@code String.class} is
	 * used as the key class in the constructor for this Calls instance.
	 *
	 * This method should be invoked from a mock method to register the mock method
	 * call.
	 *
	 * Typical use:
	 *
	 * <pre>
	 * <code>
	 * public class MyMock extends MockedClass implements Mock {
	 *    {@code Calls<String> defaultCalls} = CallsFactory.getDefaultCallsUsingStrings();
	 *    ...
	 *    {@literal @}Override
	 *    public int someMockedMethod(Object arg1, Object arg2){
	 *       calls.registerCall(arg1, arg2);
	 *       return 42;
	 *    }
	 *    ...
	 * }
	 * </code>
	 * </pre>
	 *
	 * @param args The arguments that have been used in the call to the method being
	 *             registered (cannot be extracted from the stack trace).
	 */
	public void registerCall(Object... args);

	/**
	 * Register a method call explicitely. Works for instances initialized to both
	 * {@code String.class} and {@code java.lang.reflect.Method.class} as keyClass.
	 *
	 * Typical use:
	 *
	 * <pre>
	 * <code>
	 * public class MyMock extends MockedClass implements Mock {
	 *    {@code Calls<Method> defaultCalls} = CallsFactory.getDefaultCalls();
	 *    ...
	 *    {@literal @}Override
	 *    public int someMockedMethod(Object arg1, Object arg2){
	 *       calls.registerCall(new Object(){}.getClass().getEnclosingMethod(), arg1, arg2);
	 *       return 42;
	 *    }
	 *    ...
	 * }
	 * </code>
	 * </pre>
	 *
	 * @param method The identifier for the method.
	 *
	 * @param args   The arguments that have been used in the call to the method
	 *               being registered.
	 */
	public void registerCall(T method, Object... args);

	/**
	 * Convenience method that won't be necessary for most use cases. Registers a
	 * method call explicitely. Works for instances initialized to both
	 * {@code String.class} and {@code java.lang.reflect.Method.class} as keyClass.
	 *
	 * @param method The identifier for the method.
	 * @param args   The arguments that have been used in the call to the method
	 *               being registered.
	 */
	public void registerCall(MethodCall<T> methodCall);

	public boolean verifyNoMoreMethodInvocations();

	public boolean verifyNoMoreMethodInvocations(boolean printStackTrace);

	public void traceLogMethodCall();

	public void reset();

	public boolean verifyCall(int times, T method, Object... args);

	public boolean verifyCall(int times, MethodCall<T> methodCall);

	public boolean verifyAndRemoveCall(int times, T method, Object... args);

	public boolean verifyAndRemoveCall(int times, MethodCall<T> methodCall);

	public boolean verifyStrictlyAndRemoveCall(T method, Object... args);

	public boolean verifyStrictlyAndRemoveCall(MethodCall<T> methodCall);

}
