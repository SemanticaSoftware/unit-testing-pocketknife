package com.semantica.pocketknife.mock;

import java.lang.reflect.Method;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.semantica.pocketknife.Mock;
import com.semantica.pocketknife.MocksRegistry;
import com.semantica.pocketknife.calls.Calls;
import com.semantica.pocketknife.calls.DefaultCalls;
import com.semantica.pocketknife.calls.Invoked;
import com.semantica.pocketknife.calls.Return;
import com.semantica.pocketknife.calls.StrictCalls;
import com.semantica.pocketknife.mock.InlineMocker.InlineMockers;
import com.semantica.pocketknife.util.Assert;

public class InlineMockerTest {

	private static final int METERS = 42;
	private static final String DRIVE_RETURN_VALUE = String.format("proxy rolling %dm!", METERS);

	private Car volkswagen;

	@BeforeEach
	public void setup() {
		volkswagen = new Car() {
			@Override
			public String drive(int meters) {
				return String.format("rolling %dm...", meters);
			}

			@Override
			public String park() {
				return "stalled.";
			}
		};
	}

	@Test
	public void test() {

		// Original
		Assertions.assertEquals(String.format("rolling %dm...", METERS), volkswagen.drive(METERS));
		Assertions.assertNotNull(volkswagen.park());

		// Create a mock, for now all methods return null
		InlineMocker<DefaultCalls<Method>> mocker = InlineMockers.getDefault();
		Car carMock = mocker.mock(Car.class);

		// Intercept method .drive() and make it return 'proxy roll!' (2x since the
		// mock's drive() method is invoked 2x)
		mocker.doReturn(DRIVE_RETURN_VALUE, DRIVE_RETURN_VALUE).whenIntercepted(carMock.drive(METERS));

		// Confirm the result of method drive
		Assertions.assertEquals(DRIVE_RETURN_VALUE, carMock.drive(METERS));

		mocker.assertCalled(Invoked.ONCE, carMock).drive(METERS);
		mocker.assertNoMoreMethodInvocations(carMock);

		// Method park returns null because it was not intercepted or delegated.
		Assertions.assertNull(carMock.park());

		// Delegate method calls to the object car
		mocker.delegate(Car.class, carMock, volkswagen);

		// Now park() returns 'stalled.' because it was delegated to car.park()
		Assertions.assertEquals("stalled.", carMock.park());

		// The interception prevails over delegation
		Assertions.assertEquals(DRIVE_RETURN_VALUE, carMock.drive(METERS));

	}

	@Test
	public void testnew() {

		// Original
		Assertions.assertEquals(String.format("rolling %dm...", METERS), volkswagen.drive(METERS));
		Assertions.assertNotNull(volkswagen.park());

		// Create a mock, for now all methods return null
		InlineMocker<DefaultCalls<Method>> mocker = InlineMockers.getDefault();
		Car carMock = mocker.mock(Car.class);

		// Intercept method .drive() and make it return 'proxy roll!' (2x since the
		// mock's drive() method is invoked 2x)
		/* ****2x*** */
		mocker.whenIntercepted(carMock.drive(METERS)).thenReturn(DRIVE_RETURN_VALUE, Return.TWICE);

		// Confirm the result of method drive
		Assertions.assertEquals(DRIVE_RETURN_VALUE, carMock.drive(METERS));

		mocker.assertCalled(Invoked.ONCE, carMock).drive(METERS);
		mocker.assertNoMoreMethodInvocations(carMock);

		// Method park returns null because it was not intercepted or delegated.
		Assertions.assertNull(carMock.park());

		// Delegate method calls to the object car
		mocker.delegate(Car.class, carMock, volkswagen);

		// Now park() returns 'stalled.' because it was delegated to car.park()
		Assertions.assertEquals("stalled.", carMock.park());

		// The interception prevails over delegation
		Assertions.assertEquals(DRIVE_RETURN_VALUE, carMock.drive(METERS));

	}

	@Test
	public void test2() {
		// Create a mock, for now all methods return null
		InlineMocker<DefaultCalls<Method>> mocker = InlineMockers.getDefault();
		// Original
		Assertions.assertEquals(String.format("rolling %dm...", METERS), volkswagen.drive(METERS));
		Assertions.assertNotNull(volkswagen.park());

		Car carMock = mocker.mock(Car.class);

		// Intercept method .drive() and make it return 'proxy roll!'
		mocker.whenIntercepted(carMock.drive(METERS)).thenReturn(DRIVE_RETURN_VALUE);

		// Confirm the result of method drive
		Assertions.assertEquals(DRIVE_RETURN_VALUE, carMock.drive(METERS));

		mocker.assertCalled(Invoked.ONCE, carMock).drive(METERS);
		mocker.assertNoMoreMethodInvocations(carMock);

		// Method park returns null because it was not intercepted or delegated.
		Assertions.assertNull(carMock.park());

	}

	@Test
	public void shouldVerifyMethodInvocationsStrictly() {
		InlineMocker<StrictCalls<Method>> mocker = InlineMockers.getStrict();
		Car carMock = mocker.mock(Car.class);
		mocker.whenIntercepted(carMock.drive(METERS)).thenReturn(DRIVE_RETURN_VALUE);

		Assert.actual(carMock.drive(METERS)).equalsExpected(DRIVE_RETURN_VALUE);

		mocker.assertCalled(carMock).drive(METERS);
		mocker.assertNoMoreMethodInvocations(carMock);
		mocker.assertNoMoreMethodInvocationsAnywhere();
	}

	@Test
	public void shouldVerifyMethodInvocationsStrictlyWithMatcher() {
		InlineMocker<StrictCalls<Method>> mocker = InlineMockers.getStrict();
		Car carMock = mocker.mock(Car.class);
		mocker.whenIntercepted(carMock.drive(mocker.matchArgWith(Matchers.any(Integer.class), Integer.class)))
				.thenReturn(DRIVE_RETURN_VALUE);

		Assert.actual(carMock.drive(METERS)).equalsExpected(DRIVE_RETURN_VALUE);

		mocker.assertCalled(carMock).drive(METERS);
		mocker.assertNoMoreMethodInvocations(carMock);
		mocker.assertNoMoreMethodInvocationsAnywhere();
	}

	@Test
	public void shouldReturnStubWhenArgumentMatchesMatcher() {
		InlineMocker<StrictCalls<Method>> mocker = InlineMockers.getStrict();
		Car carMock = mocker.mock(Car.class);
		mocker.whenIntercepted(carMock.drive(METERS)).thenReturn(DRIVE_RETURN_VALUE);

		Assert.actual(carMock.drive(METERS)).equalsExpected(DRIVE_RETURN_VALUE);

		mocker.assertCalled(carMock).drive(mocker.matchArgWith(distance -> distance == METERS, int.class));
		mocker.assertNoMoreMethodInvocations(carMock);
		mocker.assertNoMoreMethodInvocationsAnywhere();
	}

	@Test
	public void unverifiedmockShouldCauseVerificationToFail() {
		InlineMocker<DefaultCalls<Method>> mocker = InlineMockers.getDefault();
		Calls<?> callsMock = mocker.mock(Calls.class);
		Mock unverifiedmockMock = mocker.mock(Mock.class);

		mocker.doReturn(callsMock).when(unverifiedmockMock).getCalls();
		mocker.whenIntercepted(callsMock.verifyNoMoreMethodInvocations(false)).thenReturn(false);

		MocksRegistry mocksRegistry = new MocksRegistry();
		mocksRegistry.registerMock(unverifiedmockMock);
		assert mocksRegistry.verifyNoMoreMethodInvocationsAnywhere() == false;

		mocker.assertCalled(Invoked.ONCE, callsMock).verifyNoMoreMethodInvocations(false);
		mocker.assertCalled(Invoked.ONCE, unverifiedmockMock).getCalls();
		mocker.assertNoMoreMethodInvocations(callsMock, unverifiedmockMock);
	}
}
