package com.semantica.pocketknife.mock.it;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.semantica.pocketknife.calls.CallsFactory.CallType;
import com.semantica.pocketknife.calls.Invoked;
import com.semantica.pocketknife.calls.Return;
import com.semantica.pocketknife.mock.InlineMocker;
import com.semantica.pocketknife.mock.InlineMockers;
import com.semantica.pocketknife.mock.MockedInterface;
import com.semantica.pocketknife.mock.MockedInterfaceImplementation;

public class InlineMockerIntegrationTest {
	private static final int INT_TEST_ARGUMENT = 42;
	private static final String DRIVE_RETURN_VALUE = String.format("Received intParameter=%d in implementation.",
			INT_TEST_ARGUMENT);
	private static final String UNSTUBBED_METHOD_RETURN_VALUE = "Unstubbed return value";

	private MockedInterface implementation;

	@BeforeEach
	public void setup() {
		implementation = new MockedInterfaceImplementation();
	}

	@Test
	public void test() {

		// Original
		Assertions.assertEquals(DRIVE_RETURN_VALUE, implementation.stubbedMethod(INT_TEST_ARGUMENT));
		Assertions.assertNotNull(implementation.notStubbed());

		// Create a mock, for now all methods return null
		InlineMocker mocker = InlineMockers.get(CallType.DEFAULT);
		MockedInterface carMock = mocker.mock(MockedInterface.class);

		// Intercept method .drive() and make it return 'proxy roll!' (2x since the
		// mock's drive() method is invoked 2x)
		mocker.doReturn(DRIVE_RETURN_VALUE, DRIVE_RETURN_VALUE)
				.whenIntercepted(carMock.stubbedMethod(INT_TEST_ARGUMENT));

		// Confirm the result of method drive
		Assertions.assertEquals(DRIVE_RETURN_VALUE, carMock.stubbedMethod(INT_TEST_ARGUMENT));

		mocker.assertCalled(Invoked.ONCE, carMock).stubbedMethod(INT_TEST_ARGUMENT);
		mocker.assertNoMoreMethodInvocations(carMock);

		// Method park returns null because it was not intercepted or delegated.
		Assertions.assertNull(carMock.notStubbed());

		// Delegate method calls to the object car
		mocker.delegate(MockedInterface.class, carMock, implementation);

		// Now park() returns 'stalled.' because it was delegated to car.park()
		Assertions.assertEquals(UNSTUBBED_METHOD_RETURN_VALUE, carMock.notStubbed());

		// The interception prevails over delegation
		Assertions.assertEquals(DRIVE_RETURN_VALUE, carMock.stubbedMethod(INT_TEST_ARGUMENT));

	}

	@Test
	public void testnew() {

		// Original
		Assertions.assertEquals(DRIVE_RETURN_VALUE, implementation.stubbedMethod(INT_TEST_ARGUMENT));
		Assertions.assertNotNull(implementation.notStubbed());

		// Create a mock, for now all methods return null
		InlineMocker mocker = InlineMockers.get(CallType.DEFAULT);
		MockedInterface carMock = mocker.mock(MockedInterface.class);

		// Intercept method .drive() and make it return 'proxy roll!' (2x since the
		// mock's drive() method is invoked 2x)
		/* ****2x*** */
		mocker.whenIntercepted(carMock.stubbedMethod(INT_TEST_ARGUMENT)).thenReturn(DRIVE_RETURN_VALUE, Return.TWICE);

		// Confirm the result of method drive
		Assertions.assertEquals(DRIVE_RETURN_VALUE, carMock.stubbedMethod(INT_TEST_ARGUMENT));

		mocker.assertCalled(Invoked.ONCE, carMock).stubbedMethod(INT_TEST_ARGUMENT);
		mocker.assertNoMoreMethodInvocations(carMock);

		// Method park returns null because it was not intercepted or delegated.
		Assertions.assertNull(carMock.notStubbed());

		// Delegate method calls to the object car
		mocker.delegate(MockedInterface.class, carMock, implementation);

		// Now park() returns 'stalled.' because it was delegated to car.park()
		Assertions.assertEquals(UNSTUBBED_METHOD_RETURN_VALUE, carMock.notStubbed());

		// The interception prevails over delegation
		Assertions.assertEquals(DRIVE_RETURN_VALUE, carMock.stubbedMethod(INT_TEST_ARGUMENT));

	}

	@Test
	public void test2() {
		// Create a mock, for now all methods return null
		InlineMocker mocker = InlineMockers.get(CallType.DEFAULT);
		// Original
		Assertions.assertEquals(DRIVE_RETURN_VALUE, implementation.stubbedMethod(INT_TEST_ARGUMENT));
		Assertions.assertNotNull(implementation.notStubbed());

		MockedInterface carMock = mocker.mock(MockedInterface.class);

		// Intercept method .drive() and make it return 'proxy roll!'
		mocker.whenIntercepted(carMock.stubbedMethod(INT_TEST_ARGUMENT)).thenReturn(DRIVE_RETURN_VALUE);

		// Confirm the result of method drive
		Assertions.assertEquals(DRIVE_RETURN_VALUE, carMock.stubbedMethod(INT_TEST_ARGUMENT));

		mocker.assertCalled(Invoked.ONCE, carMock).stubbedMethod(INT_TEST_ARGUMENT);
		mocker.assertNoMoreMethodInvocations(carMock);

		// Method park returns null because it was not intercepted or delegated.
		Assertions.assertNull(carMock.notStubbed());

	}
}
