package com.semantica.pocketknife.mock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.semantica.pocketknife.mock.SimpleMock;

public class SimpleMockTest {

	SimpleMock mocker;
	static String mockedResult = "proxy roll!";

	@Test
	public void test() {
		mocker = new SimpleMock();

		Car car = new Car() {
			@Override
			public String drive() {
				return "rolling...";
			}

			@Override
			public String park() {
				return "stalled.";
			}
		};

		// Original
		Assertions.assertTrue("rolling...".equals(car.drive()));
		Assertions.assertNotNull(car.park());

		// Create a mock, for now all methods return null
		Car carMock = mocker.mock(Car.class);

		// Intercept method .drive() and make it return 'proxy roll!'
		mocker.intercept("drive", mockedResult);

		// Confirm the result of method drive
		Assertions.assertTrue(mockedResult.equals(carMock.drive()));

		// Method park returns null because it was not intercepted or delegated.
		Assertions.assertNull(carMock.park());

		// Delegate method calls to the object car
		mocker.delegate(Car.class, car);

		// Now park() returns 'stalled.' because it was delegated to car.park()
		Assertions.assertTrue("stalled.".equals(carMock.park()));

		// The interception prevails over delegation
		Assertions.assertTrue(mockedResult.equals(carMock.drive()));

	}
}
