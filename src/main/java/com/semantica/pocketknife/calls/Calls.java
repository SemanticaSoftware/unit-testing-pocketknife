package com.semantica.pocketknife.calls;

public interface Calls<T> {

	public void registerCall(Object... args);

	public void registerCall(T method, Object... args);

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
