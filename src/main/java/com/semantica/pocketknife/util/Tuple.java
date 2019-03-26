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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((s == null) ? 0 : s.hashCode());
		result = prime * result + ((t == null) ? 0 : t.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Tuple other = (Tuple) obj;
		if (s == null) {
			if (other.s != null) {
				return false;
			}
		} else if (!s.equals(other.s)) {
			return false;
		}
		if (t == null) {
			if (other.t != null) {
				return false;
			}
		} else if (!t.equals(other.t)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Tuple [s=" + s + ", t=" + t + "]";
	}

}
