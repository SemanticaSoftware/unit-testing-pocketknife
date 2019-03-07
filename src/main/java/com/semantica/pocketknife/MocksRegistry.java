package com.semantica.pocketknife;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class MocksRegistry {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MocksRegistry.class);

	private Set<Mock> mocks;
	private Set<Provider<? extends Mock>> mockProviders;

	public MocksRegistry() {
		super();
		mocks = new HashSet<>();
		mockProviders = new HashSet<>();
	}

	public MocksRegistry(Set<Mock> mocks) {
		super();
		this.mocks = mocks;
	}

	public boolean registerMock(Mock mock) {
		return mocks.add(mock);
	}

	public boolean registerMockProvider(Provider<? extends Mock> mockProvider) {
		return mockProviders.add(mockProvider);
	}

	public boolean verifyNoMoreMethodInvocationsAnywhere() {
		boolean noMoreMethodInvocationsAnywhere = true;
		for (Mock mock : mocks) {
			noMoreMethodInvocationsAnywhere &= mock.getCalls().verifyNoMoreMethodInvocations(false);
		}
		for (Provider<? extends Mock> mockProvider : mockProviders) {
			Mock mock = mockProvider.get();
			mocks.add(mock);
			noMoreMethodInvocationsAnywhere &= mock.getCalls().verifyNoMoreMethodInvocations(false);
		}
		mockProviders.clear();
		return noMoreMethodInvocationsAnywhere;
	}

	/**
	 * If the hashCode() value of a mock has changed since it was registered, the
	 * mock cannot be removed. This is due to the behaviour of HashSet.
	 *
	 * @param mock
	 * @return
	 */
	public void deregisterMock(Mock mock) {
		for (Provider<? extends Mock> mockProvider : mockProviders) {
			mocks.add(mockProvider.get());
		}
		mockProviders.clear();
		boolean success = mocks.remove(mock);
		log.debug("Result of deregistering mock \"{}\" with hash \"{}\": {}", mock.getClass().getSimpleName(),
				mock.hashCode(), success);
		if (!success) {
			throw new IllegalStateException("Mock with class " + mock.getClass().getSimpleName()
					+ " tried to deregister itself, but registration could not be found. Unable to deregister.");
		}
	}

}
