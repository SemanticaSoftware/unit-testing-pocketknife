package com.semantica.pocketknife.methodrecorder;

import java.util.Optional;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class MatchingArgument {

	private int captureNumber;
	private Object matcher;
	private Optional<Integer> argumentNumber;

	public MatchingArgument(int captureNumber, Object matcher, Optional<Integer> argumentNumber) {
		super();
		this.captureNumber = captureNumber;
		this.matcher = matcher;
		this.argumentNumber = argumentNumber;
	}

	public int getCaptureNumber() {
		return captureNumber;
	}

	public void setCaptureNumber(int captureNumber) {
		this.captureNumber = captureNumber;
	}

	public Optional<Integer> getArgumentNumber() {
		return argumentNumber;
	}

	public void setArgumentNumber(Optional<Integer> argumentNumber) {
		this.argumentNumber = argumentNumber;
	}

	public Object getMatcher() {
		return matcher;
	}

	public void setMatcher(Object matcher) {
		this.matcher = matcher;
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
