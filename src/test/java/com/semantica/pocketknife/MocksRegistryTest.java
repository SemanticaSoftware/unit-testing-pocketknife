package com.semantica.pocketknife;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.semantica.pocketknife.calls.Calls;

public class MocksRegistryTest {

	@Test
	public void verifiedmockShouldCauseVerificationToFail() {
		Calls<?> callsMock = Mockito.mock(Calls.class);
		Mock verifiedmockMock = Mockito.mock(Mock.class);
		Mockito.doReturn(callsMock).when(verifiedmockMock).getCalls();
		Mockito.when(callsMock.verifyNoMoreMethodInvocations(false)).thenReturn(true);

		MocksRegistry mocksRegistry = new MocksRegistry();
		mocksRegistry.registerMock(verifiedmockMock);
		assert mocksRegistry.verifyNoMoreMethodInvocationsAnywhere();

		Mockito.verify(callsMock, Mockito.times(1)).verifyNoMoreMethodInvocations(false);
		Mockito.verify(verifiedmockMock, Mockito.times(1)).getCalls();
		Mockito.verifyNoMoreInteractions(callsMock, verifiedmockMock);
	}

	@Test
	public void unverifiedmockShouldCauseVerificationToFail() {
		Calls<?> callsMock = Mockito.mock(Calls.class);
		Mock unverifiedmockMock = Mockito.mock(Mock.class);
		Mockito.doReturn(callsMock).when(unverifiedmockMock).getCalls();
		Mockito.when(callsMock.verifyNoMoreMethodInvocations(false)).thenReturn(false);

		MocksRegistry mocksRegistry = new MocksRegistry();
		mocksRegistry.registerMock(unverifiedmockMock);
		assert mocksRegistry.verifyNoMoreMethodInvocationsAnywhere() == false;

		Mockito.verify(callsMock, Mockito.times(1)).verifyNoMoreMethodInvocations(false);
		Mockito.verify(unverifiedmockMock, Mockito.times(1)).getCalls();
		Mockito.verifyNoMoreInteractions(callsMock, unverifiedmockMock);
	}

	@Test
	public void providerWithVerifiedmockShouldCauseVerificationToFail() {
		Calls<?> callsMock = Mockito.mock(Calls.class);
		Mock verifiedmockMock = Mockito.mock(Mock.class);
		Mockito.doReturn(callsMock).when(verifiedmockMock).getCalls();
		Mockito.when(callsMock.verifyNoMoreMethodInvocations(false)).thenReturn(true);

		MocksRegistry mocksRegistry = new MocksRegistry();
		mocksRegistry.registerMock(new Provider<Mock>() {

			@Override
			public Mock get() {
				return verifiedmockMock;
			}

		});
		assert mocksRegistry.verifyNoMoreMethodInvocationsAnywhere();

		Mockito.verify(callsMock, Mockito.times(1)).verifyNoMoreMethodInvocations(false);
		Mockito.verify(verifiedmockMock, Mockito.times(1)).getCalls();
		Mockito.verifyNoMoreInteractions(callsMock, verifiedmockMock);
	}

}
