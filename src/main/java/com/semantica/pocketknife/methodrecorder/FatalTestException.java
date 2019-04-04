package com.semantica.pocketknife.methodrecorder;

public class FatalTestException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public FatalTestException(Throwable cause) {
		super(cause);
	}

	public FatalTestException(String message, Throwable cause) {
		super(message, cause);
	}
}