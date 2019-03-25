package com.semantica.pocketknife.calls;

/**
 * The default {@link DefaultCalls} implementation. Features non-strict method
 * verification and allows its user to verify multiple equivalent method calls
 * simultaneously by specifying the number of times a method is expected to have
 * been called.
 *
 * @author A. Haanstra
 *
 * @param <T>
 */
class CallsRegistry<T> extends AbstractCallsRegistry<T> implements DefaultCalls<T> {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CallsRegistry.class);

	protected CallsRegistry(Class<T> methodClass) {
		super(methodClass);
	}

	@Override
	public boolean verifyCall(int times, T method, Object... args) {
		MethodCall<T> methodCall = new MethodCall<>(method, args);
		return isCalled(times, methodCall, false);
	}

	@Override
	public boolean verifyCall(int times, MethodCall<T> methodCall) {
		return isCalled(times, methodCall, false);
	}

	@Override
	public boolean verifyAndRemoveCall(int times, T method, Object... args) {
		MethodCall<T> methodCall = new MethodCall<>(method, args);
		return isCalled(times, methodCall, true);
	}

	@Override
	public boolean verifyAndRemoveCall(int times, MethodCall<T> methodCall) {
		return isCalled(times, methodCall, true);
	}

	@Override
	public boolean verifyCall(Invoked timesInvoked, T method, Object... args) {
		return verifyCall(timesInvoked.getTimes(), method, args);
	}

	@Override
	public boolean verifyCall(Invoked timesInvoked, MethodCall<T> methodCall) {
		return verifyCall(timesInvoked.getTimes(), methodCall);
	}

	@Override
	public boolean verifyAndRemoveCall(Invoked timesInvoked, T method, Object... args) {
		return verifyAndRemoveCall(timesInvoked.getTimes(), method, args);
	}

	@Override
	public boolean verifyAndRemoveCall(Invoked timesInvoked, MethodCall<T> methodCall) {
		return verifyAndRemoveCall(timesInvoked.getTimes(), methodCall);
	}

	private boolean isCalled(int expectedTimes, MethodCall<T> queryMethodCall, boolean removeCall) {
		boolean isCalled;
		MethodCall<T> foundMethodCall = getStoredExactMethodCall(queryMethodCall);
		final MethodCall<T> methodCall = foundMethodCall == null ? queryMethodCall : foundMethodCall;
		Integer actualTimes = calls.get(methodCall) == null ? 0 : calls.get(methodCall).size();
		if ((actualTimes == null || actualTimes == 0) && expectedTimes == 0) {
			isCalled = true;
		} else if (actualTimes != null && actualTimes == expectedTimes) {
			isCalled = true;
		} else {
			isCalled = false;
			log.error("Method {} was invoked {} {} x, while {} x was expected.", methodCall.getMethod(),
					methodCall.getArgs().length == 0 ? "without arguments:"
							: "with arguments [" + getCommaSeparatedArgs(methodCall) + "]:",
					actualTimes == null ? 0 : actualTimes, expectedTimes);
			log.info("Registered invocations for method {}:{}{}", methodCall.getMethod(), System.lineSeparator(),
					getNewlineSeperatedCalls((entry) -> methodCall.getMethod().equals(entry.getKey().getMethod()),
							true));
		}
		if (isCalled && removeCall) {
			calls.remove(methodCall);
		}
		return isCalled;
	}

}
