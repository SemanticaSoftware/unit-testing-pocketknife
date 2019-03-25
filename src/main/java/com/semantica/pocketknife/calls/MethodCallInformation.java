package com.semantica.pocketknife.calls;

/**
 * Data class storing the unqiue information for a method call. Stores the
 * caller information (stack trace) and sequential invocation number for this
 * mock.
 *
 * @author A. Haanstra
 *
 */
class MethodCallInformation {

	private StackTraceElement[] stackTraceElements;
	private int methodInvocationSequenceNo;

	public MethodCallInformation(StackTraceElement[] stackTraceElements, int methodInvocationSequenceNo) {
		super();
		this.stackTraceElements = stackTraceElements;
		this.methodInvocationSequenceNo = methodInvocationSequenceNo;
	}

	public StackTraceElement[] getStackTraceElements() {
		return stackTraceElements;
	}

	public int getMethodInvocationSequenceNo() {
		return methodInvocationSequenceNo;
	}

}
