package com.semantica.pocketknife.mock;

import java.lang.reflect.Method;

import com.semantica.pocketknife.calls.Calls;
import com.semantica.pocketknife.calls.Invoked;
import com.semantica.pocketknife.mock.dto.QualifiedMethodCall;

public interface VerificationStore<T extends Calls<Method>> {
	public <S> void newCallsRegistryFor(Object proxy);

	public void removeCall(QualifiedMethodCall<Method> qualifiedMethodCall);

	public void assertCalled(QualifiedMethodCall<Method> qualifiedMatchingMethod);

	public void registerCall(QualifiedMethodCall<Method> qualifiedMethodCall);

	public void assertNoMoreMethodInvocations(Object... mocks);

	public void assertNoMoreMethodInvocationsAnywhere();

	public void addNumberOfTimesIncomingMethodIsExpectedToBeInvoked(Invoked timesInvoked);
}