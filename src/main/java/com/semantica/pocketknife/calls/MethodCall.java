package com.semantica.pocketknife.calls;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Data class representing a method call. Next to the method signature
 * (represented by the method field when T is {@link java.lang.reflect.Method}),
 * it also stores the actual arguments a method was called with.
 *
 * @author A. Haanstra
 *
 * @param <T>
 */
public class MethodCall<T> {

	private final T method;
	private final Object[] args;

	public MethodCall(T method, Object... args) {
		super();
		this.method = method;
		this.args = args;
	}

	public T getMethod() {
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
