package com.semantica.pocketknife.calls;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;

public class StrictCalls<T> extends AbstractCalls<T> implements Calls<T> {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StrictCalls.class);
	private int sequentialCallVerificationNo = 0;

	protected StrictCalls(Class<T> methodClass) {
		super(methodClass);
	}

	@Override
	public boolean verifyStrictlyAndRemoveCall(T method, Object... args) {
		MethodCall<T> methodCall = new MethodCall<>(method, args);
		return isSequentiallyCalled(methodCall);
	}

	@Override
	public boolean verifyStrictlyAndRemoveCall(MethodCall<T> methodCall) {
		return isSequentiallyCalled(methodCall);
	}

	protected boolean isSequentiallyCalled(MethodCall<T> queryMethodCall) {
		boolean isSequentiallyCalled;
		MethodCall<T> foundMethodCall = getStoredExactMethodCall(queryMethodCall);
		List<MethodCallInformation> callInfoUnfiltered = null, callInfoFiltered = null;
		if (foundMethodCall != null) {
			callInfoUnfiltered = calls.get(foundMethodCall);
			callInfoFiltered = callInfoUnfiltered.stream()
					.filter(info -> info.getMethodInvocationSequenceNo() == sequentialCallVerificationNo)
					.collect(Collectors.toList());
			if (callInfoFiltered.size() == 0) {
				log.error("Registered invocations for method {}:{}{}", foundMethodCall.getMethod(),
						System.lineSeparator(), getNewlineSeperatedCalls(
								(entry) -> foundMethodCall.getMethod().equals(entry.getKey().getMethod()), true));
				isSequentiallyCalled = false;
			} else if (callInfoFiltered.size() > 1) {
				log.error("Registered invocations for method {}:{}{}", foundMethodCall.getMethod(),
						System.lineSeparator(), getNewlineSeperatedCalls(
								(entry) -> foundMethodCall.getMethod().equals(entry.getKey().getMethod()), true));
				throw new IllegalStateException(
						"Multiple registered method calls found with the same invocation sequence number.");
			} else { // size == 1
				sequentialCallVerificationNo++;
				isSequentiallyCalled = true;
			}
		} else {
			isSequentiallyCalled = false;
		}
		if (isSequentiallyCalled) {
			callInfoUnfiltered.remove(callInfoFiltered.get(0));
			if (callInfoUnfiltered.size() == 0) {
				calls.remove(foundMethodCall);
			}
		}
		return isSequentiallyCalled;
	}

	public void reset() {
		super.reset();
		sequentialCallVerificationNo = 0;
	}

	@Override
	public boolean verifyCall(int times, T method, Object... args) {
		throw new NotImplementedException("Default call verification is not available for this "
				+ this.getClass().getSimpleName() + " instance. Please use verifyStrictlyAndRemoveCall(..).");
	}

	@Override
	public boolean verifyCall(int times, MethodCall<T> methodCall) {
		throw new NotImplementedException("Default call verification is not available for this "
				+ this.getClass().getSimpleName() + " instance. Please use verifyStrictlyAndRemoveCall(..).");
	}

	@Override
	public boolean verifyAndRemoveCall(int times, T method, Object... args) {
		throw new NotImplementedException("Default call verification is not available for this "
				+ this.getClass().getSimpleName() + " instance. Please use verifyStrictlyAndRemoveCall(..).");
	}

	@Override
	public boolean verifyAndRemoveCall(int times, MethodCall<T> methodCall) {
		throw new NotImplementedException("Default call verification is not available for this "
				+ this.getClass().getSimpleName() + " instance. Please use verifyStrictlyAndRemoveCall(..).");
	}
}
