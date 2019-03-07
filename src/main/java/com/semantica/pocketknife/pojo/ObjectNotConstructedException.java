package com.semantica.pocketknife.pojo;

public class ObjectNotConstructedException extends Exception {

	private static final long serialVersionUID = 1L;

	public ObjectNotConstructedException() {
		super();
	}

	public ObjectNotConstructedException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ObjectNotConstructedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ObjectNotConstructedException(String message) {
		super(message);
	}

	public ObjectNotConstructedException(Throwable cause) {
		super(cause);
	}

}
