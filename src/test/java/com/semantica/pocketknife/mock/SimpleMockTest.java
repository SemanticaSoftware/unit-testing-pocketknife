package com.semantica.pocketknife.mock;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.semantica.pocketknife.calls.CallsFactory;
import com.semantica.pocketknife.calls.DefaultCalls;
import com.semantica.pocketknife.calls.Invoked;
import com.semantica.pocketknife.calls.MethodCall;
import com.semantica.pocketknife.methodrecorder.MethodRecorder;

public class SimpleMockTest {

	SimpleMock mocker;

	static final int METERS = 42;
	static String mockedResult = String.format("proxy rolling %dm!", METERS);

	@Test
	public void test() {
		mocker = new SimpleMock();

		Car car = new Car() {
			@Override
			public String drive(int meters) {
				return String.format("rolling %dm...", meters);
			}

			@Override
			public String park() {
				return "stalled.";
			}
		};

		// Original
		Assertions.assertEquals(String.format("rolling %dm...", METERS), car.drive(METERS));
		Assertions.assertNotNull(car.park());

		// Create a mock, for now all methods return null
		DefaultCalls<Method> carCalls = CallsFactory.getDefaultCalls();
		MethodRecorder<Car> carMethodRecorder = MethodRecorder.recordInvocationsOn(Car.class);
		Car carMock = mocker.mock(Car.class, carCalls);

		// Intercept method .drive() and make it return 'proxy roll!'
		MethodCall<Method> drive = carMethodRecorder.getMethodCall(carMethodRecorder.getProxy().drive(METERS));
		mocker.intercept(drive, mockedResult);

		// Confirm the result of method drive
		Assertions.assertTrue(mockedResult.equals(carMock.drive(METERS)));

		// Method park returns null because it was not intercepted or delegated.
		Assertions.assertNull(carMock.park());

		carCalls.verifyAndRemoveCall(Invoked.ONCE, drive);
		carCalls.verifyNoMoreMethodInvocations();

		// Delegate method calls to the object car
		mocker.delegate(Car.class, car);

		// Now park() returns 'stalled.' because it was delegated to car.park()
		Assertions.assertTrue("stalled.".equals(carMock.park()));

		// The interception prevails over delegation
		Assertions.assertTrue(mockedResult.equals(carMock.drive(METERS)));

	}
}
