package com.semantica.pocketknife;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * This class can be used as a registry for all mocks in a particular unit test.
 * It serves to provide a simple one-call check to see whether all mock
 * invocations have been verified on all registered mocks. Typically, all mocks
 * register themselves, the unit test is executed, mock invocations are verified
 * and at the end it is verified that no unexepcted call were made by calling
 * {@link #verifyNoMoreMethodInvocationsAnywhere()
 * verifyNoMoreMethodInvocationsAnywhere}.
 *
 * @author A. Haanstra
 *
 */
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

	/**
	 * Creates a new {@link MocksRegistryTest} with the given mocks.
	 *
	 * @param mocks Set of mocks that need to be registered.
	 */
	public MocksRegistry(Set<Mock> mocks) {
		super();
		this.mocks = mocks;
	}

	/**
	 * Registers a mock.
	 *
	 * @param mock The mock object to be registered.
	 * @return
	 */
	public boolean registerMock(Mock mock) {
		return mocks.add(mock);
	}

	/**
	 * Registers a provider of a mock. To facilitate lazy retrieval of an injected
	 * dependency.
	 *
	 * @param mockProvider Provider of a mock to be registered.
	 * @return
	 */
	public boolean registerMock(Provider<? extends Mock> mockProvider) {
		return mockProviders.add(mockProvider);
	}

	/**
	 * Verifies that no more method invocations have occurred on the registered
	 * mocks than those that have been verified.
	 *
	 * @return {@code true} if all method invocations have been verified and
	 *         removed, {@code false} otherwise.
	 */
	public boolean verifyNoMoreMethodInvocationsAnywhere() {
		boolean noMoreMethodInvocationsAnywhere = true;
		for (Provider<? extends Mock> mockProvider : mockProviders) {
			Mock mock = mockProvider.get();
			mocks.add(mock);
		}
		mockProviders.clear();
		for (Mock mock : mocks) {
			noMoreMethodInvocationsAnywhere &= mock.getCalls().verifyNoMoreMethodInvocations(false);
		}
		return noMoreMethodInvocationsAnywhere;
	}

	/**
	 * Removes a mocks from the list of registered mocks. If the hashCode() value of
	 * a mock has changed since it was registered, the mock cannot be removed. This
	 * is due to the behaviour of HashSet.
	 *
	 * @param mock The mock that is deregistered.
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
