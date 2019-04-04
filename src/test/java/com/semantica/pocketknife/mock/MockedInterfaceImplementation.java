package com.semantica.pocketknife.mock;

public class MockedInterfaceImplementation implements MockedInterface {
	@Override
	public String stubbedMethod(int intParameter) {
		System.out.println("In unstubbed method...");
		return String.format("Received intParameter=%d in implementation.", intParameter);
	}

	@Override
	public String notStubbed() {
		return "Unstubbed return value";
	}
};