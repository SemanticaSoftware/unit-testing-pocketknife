package com.semantica.pocketknife.mock;

import com.semantica.pocketknife.calls.MethodCall;

public class QualifiedMethodCall<T> {

	private final Object invokedOnInstance;
	private final MethodCall<T> methodCall;

	public QualifiedMethodCall(Object invokedOnInstance, MethodCall<T> methodCall) {
		super();
		this.invokedOnInstance = invokedOnInstance;
		this.methodCall = methodCall;
	}

	public Object getInvokedOnInstance() {
		return invokedOnInstance;
	}

	public MethodCall<T> getMethodCall() {
		return methodCall;
	}

}
