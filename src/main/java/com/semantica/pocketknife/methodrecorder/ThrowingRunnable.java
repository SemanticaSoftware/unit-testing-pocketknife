package com.semantica.pocketknife.methodrecorder;

@FunctionalInterface
public interface ThrowingRunnable {

	public void run() throws Exception;

}
