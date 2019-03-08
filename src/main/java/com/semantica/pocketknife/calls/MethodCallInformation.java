package com.semantica.pocketknife.calls;

public class MethodCallInformation {

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
