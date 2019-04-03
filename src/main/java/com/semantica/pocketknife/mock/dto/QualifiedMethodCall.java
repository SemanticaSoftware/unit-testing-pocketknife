package com.semantica.pocketknife.mock.dto;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

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

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	@Override
	public boolean equals(Object other) {
		return EqualsBuilder.reflectionEquals(this, other);
	}
}
