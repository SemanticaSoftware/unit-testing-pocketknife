package com.semantica.pocketknife.mock;

import java.util.List;

public class AlternativeStubber<U> {
	private final List<Object> returnValues;

	AlternativeStubber(List<Object> returnValues) {
		super();
		this.returnValues = returnValues;
	}

	public AlternativeStubber<U> whenIntercepted(U dummy) {
		return this;
	}

	public <V> V when(V mock) {
		return mock;
	}

	List<Object> getReturnValues() {
		return this.returnValues;
	}

}