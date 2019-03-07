package com.semantica.pocketknife;

@FunctionalInterface
public interface ThrowingRunnable {

	public void run() throws Exception;

}
