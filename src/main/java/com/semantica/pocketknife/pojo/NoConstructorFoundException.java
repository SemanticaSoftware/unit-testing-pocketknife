package com.semantica.pocketknife.pojo;

class NoConstructorFoundException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public NoConstructorFoundException() {
		super();
	}

	public NoConstructorFoundException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public NoConstructorFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoConstructorFoundException(String message) {
		super(message);
	}

	public NoConstructorFoundException(Throwable cause) {
		super(cause);
	}

}