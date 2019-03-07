package com.semantica.pocketknife;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class MethodCall {

	private final Object method;
	private final Object[] args;

	public MethodCall(Object method, Object[] args) {
		super();
		this.method = method;
		this.args = args;
	}

	public Object getMethod() {
		return method;
	}

	public Object[] getArgs() {
		return args;
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
