package com.semantica.pocketknife;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.semantica.pocketknife.calls.Calls;

public class MocksRegistryTest {

	@Test
	public void verifiedmockShouldCauseVerificationToSucceed() {
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
	public void providerWithVerifiedmockShouldCauseVerificationToSucceed() {
		Calls<?> callsMock = Mockito.mock(Calls.class);
		Mock verifiedmockMock = Mockito.mock(Mock.class);
		Mockito.doReturn(callsMock).when(verifiedmockMock).getCalls();
		Mockito.when(callsMock.verifyNoMoreMethodInvocations(false)).thenReturn(true);

		MocksRegistry mocksRegistry = new MocksRegistry();
		mocksRegistry.registerMock(() -> verifiedmockMock);
		assert mocksRegistry.verifyNoMoreMethodInvocationsAnywhere();

		Mockito.verify(callsMock, Mockito.times(1)).verifyNoMoreMethodInvocations(false);
		Mockito.verify(verifiedmockMock, Mockito.times(1)).getCalls();
		Mockito.verifyNoMoreInteractions(callsMock, verifiedmockMock);
	}

	@Test
	public void removedUnverifiedmockShouldNotCauseVerificationToFail() {
		Calls<?> callsMock = Mockito.mock(Calls.class);
		Mock unverifiedmockMock = Mockito.mock(Mock.class);
		Mockito.doReturn(callsMock).when(unverifiedmockMock).getCalls();
		Mockito.when(callsMock.verifyNoMoreMethodInvocations(false)).thenReturn(false);

		MocksRegistry mocksRegistry = new MocksRegistry();
		mocksRegistry.registerMock(unverifiedmockMock);
		mocksRegistry.deregisterMock(unverifiedmockMock);
		assert mocksRegistry.verifyNoMoreMethodInvocationsAnywhere();

		Mockito.verifyNoMoreInteractions(callsMock, unverifiedmockMock);
	}

	@Test
	public void removedUnverifiedmockFromProviderShouldNotCauseVerificationToFail() {
		Calls<?> callsMock = Mockito.mock(Calls.class);
		Mock unverifiedmockMock = Mockito.mock(Mock.class);
		Mockito.doReturn(callsMock).when(unverifiedmockMock).getCalls();
		Mockito.when(callsMock.verifyNoMoreMethodInvocations(false)).thenReturn(false);

		MocksRegistry mocksRegistry = new MocksRegistry();
		mocksRegistry.registerMock(() -> unverifiedmockMock);
		mocksRegistry.deregisterMock(unverifiedmockMock);
		assert mocksRegistry.verifyNoMoreMethodInvocationsAnywhere();

		Mockito.verifyNoMoreInteractions(callsMock, unverifiedmockMock);
	}

	@Test
	public void verifiedmocksShouldCauseStaticVerificationToSucceed() {
		Calls<?> callsMock1 = Mockito.mock(Calls.class);
		Mock verifiedmockMock1 = Mockito.mock(Mock.class);
		Mockito.doReturn(callsMock1).when(verifiedmockMock1).getCalls();
		Mockito.when(callsMock1.verifyNoMoreMethodInvocations(false)).thenReturn(true);

		Calls<?> callsMock2 = Mockito.mock(Calls.class);
		Mock verifiedmockMock2 = Mockito.mock(Mock.class);
		Mockito.doReturn(callsMock2).when(verifiedmockMock2).getCalls();
		Mockito.when(callsMock2.verifyNoMoreMethodInvocations(false)).thenReturn(true);
		assert MocksRegistry.verifyNoMoreMethodInvocations(verifiedmockMock1, verifiedmockMock2);

		Mockito.verify(callsMock1, Mockito.times(1)).verifyNoMoreMethodInvocations(false);
		Mockito.verify(verifiedmockMock1, Mockito.times(1)).getCalls();
		Mockito.verify(callsMock2, Mockito.times(1)).verifyNoMoreMethodInvocations(false);
		Mockito.verify(verifiedmockMock2, Mockito.times(1)).getCalls();
		Mockito.verifyNoMoreInteractions(callsMock1, callsMock2, verifiedmockMock1, verifiedmockMock2);
	}

}
