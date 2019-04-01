package com.semantica.pocketknife.mock;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;

import com.semantica.pocketknife.calls.Calls;
import com.semantica.pocketknife.calls.DefaultCalls;
import com.semantica.pocketknife.calls.Invoked;
import com.semantica.pocketknife.calls.MethodCall;
import com.semantica.pocketknife.calls.StrictCalls;

public class InlineMockerCallRegistriesStore<T extends Calls<Method>> implements InlineMocker.CallRegistriesStore<T> {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
			.getLogger(InlineMockerCallRegistriesStore.class);
	// key: the proxy (mock) instance
	private final Map<Object, T> allCallsRegistries = new HashMap<>();
	private final Class<T> callsClass;

	public InlineMockerCallRegistriesStore(Class<T> callsClass) {
		super();
		this.callsClass = callsClass;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void newCallsRegistryFor(Object proxy) {
		T calls = null;
		if (DefaultCalls.class.isAssignableFrom(callsClass)) {
			calls = (T) new DefaultMockCallsRegistry<>(Method.class);
		} else if (StrictCalls.class.isAssignableFrom(callsClass)) {
			calls = (T) new StrictMockCallsRegistry<>(Method.class);
		} else {
			throwNotImplementedExceptionForCallsClass();
		}
		allCallsRegistries.put(proxy, calls);
	}

	private void throwNotImplementedExceptionForCallsClass() {
		throw new NotImplementedException(
				String.format("Calls class %s is unknown and not implemented for %s.", callsClass, this.getClass()));
	}

	@Override
	public void removeCall(QualifiedMethodCall<Method> qualifiedMethodCall) {
		Calls<Method> calls = allCallsRegistries.get(qualifiedMethodCall.getInvokedOnInstance());
		calls.removeCall(qualifiedMethodCall.getMethodCall());
	}

	@Override
	public void assertCalled(Invoked numberOfTimesIncomingMethodIsExpectedToBeInvoked,
			QualifiedMethodCall<Method> qualifiedMatchingMethod) {
		MethodCall<Method> matchingMethod = qualifiedMatchingMethod.getMethodCall();
		T calls = allCallsRegistries.get(qualifiedMatchingMethod.getInvokedOnInstance());
		if (DefaultCalls.class.isAssignableFrom(callsClass)) {
			assert ((DefaultCalls<Method>) calls).verifyAndRemoveCall(numberOfTimesIncomingMethodIsExpectedToBeInvoked,
					matchingMethod);
		} else if (StrictCalls.class.isAssignableFrom(callsClass)) {
			if (numberOfTimesIncomingMethodIsExpectedToBeInvoked.equals(Invoked.ONCE)) {
				assert ((StrictCalls<Method>) calls).verifyAndRemoveCall(matchingMethod);
			} else {
				log.error(
						"Requested to verify qualified method call \"{}\" {}x, but this is not allowed in strict call verification mode.",
						qualifiedMatchingMethod, numberOfTimesIncomingMethodIsExpectedToBeInvoked.getTimes());
				throw new IllegalArgumentException(String.format(
						"For strict call verification, method calls should be verified one at a time.%nERROR: Requested to verify qualified method call \"%s\" %dx, but this is not allowed in strict call verification mode.",
						qualifiedMatchingMethod, numberOfTimesIncomingMethodIsExpectedToBeInvoked.getTimes()));
			}
		} else {
			throwNotImplementedExceptionForCallsClass();
		}
	}

	@Override
	public void registerCall(QualifiedMethodCall<Method> qualifiedMethodCall) {
		T calls = allCallsRegistries.get(qualifiedMethodCall.getInvokedOnInstance());
		calls.registerCall(qualifiedMethodCall.getMethodCall());
	}

	@Override
	public void assertNoMoreMethodInvocations(Object... mocks) {
		for (Object mock : mocks) {
			assert allCallsRegistries.get(mock).verifyNoMoreMethodInvocations();
		}
	}

	@Override
	public void assertNoMoreMethodInvocationsAnywhere() {
		for (T calls : allCallsRegistries.values()) {
			assert calls.verifyNoMoreMethodInvocations();
		}
	}

}
