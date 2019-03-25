package com.semantica.pocketknife.util;

public class Tuple<S, T> {
	private final S s;
	private final T t;

	public Tuple(S s, T t) {
		super();
		this.s = s;
		this.t = t;
	}

	public S getS() {
		return s;
	}

	public T getT() {
		return t;
	}

}
