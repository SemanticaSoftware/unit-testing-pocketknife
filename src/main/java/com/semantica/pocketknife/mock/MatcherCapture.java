package com.semantica.pocketknife.mock;

import java.util.Optional;

public class MatcherCapture<T> {

	private Object matcher;
	private Class<T> clazz;
	private Optional<Integer> argumentNumber = Optional.empty();
	private T wiringIdentity;

	public MatcherCapture(Object matcher, Class<T> clazz, T wiringIdentity) {
		this(matcher, clazz, Optional.empty(), wiringIdentity);
	}

	public MatcherCapture(Object matcher, Class<T> clazz, Optional<Integer> argumentNumber, T wiringIdentity) {
		super();
		this.matcher = matcher;
		this.clazz = clazz;
		this.argumentNumber = argumentNumber;
		this.wiringIdentity = wiringIdentity;
	}

	public Object getMatcher() {
		return matcher;
	}

	public Class<T> getClazz() {
		return clazz;
	}

	public Optional<Integer> getArgumentNumber() {
		return argumentNumber;
	}

	public T getWiringIdentity() {
		return wiringIdentity;
	}
}
