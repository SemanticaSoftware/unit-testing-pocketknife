package com.semantica.pocketknife;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;

public class Calls {

	// TODO: Allow registration and verification of arguments used with method call

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Calls.class);
	// Object can be: Method or String (method name)
	private Map<MethodCall, List<MethodCallInformation>> calls = new HashMap<>();
	private final Class<?> keyClass;
	private int sequentialCallNo = 0;
	private int sequentialCallVerificationNo = 0;

	/**
	 * Creates a Calls registry that is initialized to the given key class.
	 *
	 * @param methodClass This class determines how methods will be stored, can be
	 *                    either String.class or Method.class
	 */
	public Calls(Class<?> methodClass) {
		super();
		if (methodClass != String.class && methodClass != Method.class) {
			throw new IllegalArgumentException(
					"Unsupported method class used. Use either String.class or Method.class.");
		}
		this.keyClass = methodClass;
	}

	public void registerCall(Object... args) {
		checkInitializedWithStringKeyClass();
		traceLogMethodCall();
		String methodName = getMethodName(1);
		MethodCall methodCall = new MethodCall(methodName, args);
		addStackTraceToCalls(methodCall, Thread.currentThread().getStackTrace());
	}

	public void registerCall(Method method, Object... args) {
		checkInitializedWithMethodKeyClass();
		MethodCall methodCall = new MethodCall(method, args);
		addStackTraceToCalls(methodCall, Thread.currentThread().getStackTrace());
	}

//sequentialCallNo++
	private void addStackTraceToCalls(MethodCall methodCall, StackTraceElement[] stackTrace) {
		List<MethodCallInformation> stackTraces = calls.get(methodCall);
		if (stackTraces == null) {
			stackTraces = new ArrayList<>();
		}
		stackTraces.add(new MethodCallInformation(stackTrace, sequentialCallNo++));
		calls.put(methodCall, stackTraces);
	}

	/**
	 * Get the method name for a depth in call stack.
	 *
	 * @param depth depth in the call stack (0 means current method, 1 means
	 *              invoking method, ...)
	 * @return method name
	 */
	private static String getMethodName(final int depth) {
		final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		return ste[2 + depth].getMethodName();
	}

	public boolean verifyCall(int times, String methodName, Object... args) {
		checkInitializedWithStringKeyClass();
		MethodCall methodCall = new MethodCall(methodName, args);
		return isCalled(times, methodCall, false);
	}

	public boolean verifyCall(int times, Method method, Object... args) {
		checkInitializedWithMethodKeyClass();
		MethodCall methodCall = new MethodCall(method, args);
		return isCalled(times, methodCall, false);
	}

	public boolean verifyCall(int times, MethodCall methodCall) {
		checkInitializedWithMethodKeyClass();
		return isCalled(times, methodCall, false);
	}

	public boolean verifyAndRemoveCall(int times, String methodName, Object... args) {
		checkInitializedWithStringKeyClass();
		MethodCall methodCall = new MethodCall(methodName, args);
		return isCalled(times, methodCall, true);
	}

	public boolean verifyAndRemoveCall(int times, Method method, Object... args) {
		checkInitializedWithMethodKeyClass();
		MethodCall methodCall = new MethodCall(method, args);
		return isCalled(times, methodCall, true);
	}

	public boolean verifyAndRemoveCall(int times, MethodCall methodCall) {
		checkInitializedWithMethodKeyClass();
		return isCalled(times, methodCall, true);
	}

	public boolean verifyStrictlyAndRemoveCall(String methodName, Object... args) {
		checkInitializedWithStringKeyClass();
		MethodCall methodCall = new MethodCall(methodName, args);
		return isSequentiallyCalled(methodCall);
	}

	public boolean verifyStrictlyAndRemoveCall(Method method, Object... args) {
		checkInitializedWithMethodKeyClass();
		MethodCall methodCall = new MethodCall(method, args);
		return isSequentiallyCalled(methodCall);
	}

	public boolean verifyStrictlyAndRemoveCall(MethodCall methodCall) {
		checkInitializedWithMethodKeyClass();
		return isSequentiallyCalled(methodCall);
	}

	private boolean isSequentiallyCalled(MethodCall queryMethodCall) {
		boolean isSequentiallyCalled;
		MethodCall foundMethodCall = getStoredExactMethodCall(queryMethodCall);
		if (foundMethodCall != null) {
			List<MethodCallInformation> callInfo = calls.get(foundMethodCall).stream()
					.filter(info -> info.getMethodInvocationSequenceNo() == sequentialCallVerificationNo)
					.collect(Collectors.toList());
			if (callInfo.size() == 0) {
				log.error("Registered invocations for method {}:{}{}", foundMethodCall.getMethod(),
						System.lineSeparator(), getNewlineSeperatedCalls(
								(entry) -> foundMethodCall.getMethod().equals(entry.getKey().getMethod()), true));
				isSequentiallyCalled = false;
			} else if (callInfo.size() != 1) {
				log.error("Registered invocations for method {}:{}{}", foundMethodCall.getMethod(),
						System.lineSeparator(), getNewlineSeperatedCalls(
								(entry) -> foundMethodCall.getMethod().equals(entry.getKey().getMethod()), true));
				throw new IllegalStateException(
						"Multiple registered method calls found with the same invocation sequence number.");
			} else {
				sequentialCallVerificationNo++;
				isSequentiallyCalled = true;
			}
		} else {
			isSequentiallyCalled = false;
		}
		if (isSequentiallyCalled) {
			calls.remove(foundMethodCall);
		}
		return isSequentiallyCalled;
	}

	private boolean isCalled(int expectedTimes, MethodCall queryMethodCall, boolean removeCall) {
		boolean isCalled;
		MethodCall foundMethodCall = getStoredExactMethodCall(queryMethodCall);
		final MethodCall methodCall = foundMethodCall == null ? queryMethodCall : foundMethodCall;
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

	private MethodCall getStoredExactMethodCall(MethodCall methodCall) {
		Set<MethodCall> registeredMethodCalls = calls.keySet();
		final MethodCall queryMethodCall = methodCall;
		List<MethodCall> matchingCalls = registeredMethodCalls.stream()
				.filter(registeredCall -> registeredCall.getMethod().equals(queryMethodCall.getMethod()))
				.filter(registeredCall -> registeredCall.getArgs().length == queryMethodCall.getArgs().length)
				.filter(registeredCall -> match(registeredCall, queryMethodCall)).collect(Collectors.toList());
		if (matchingCalls.size() > 1) {
			throw new IllegalArgumentException("The methodCall was ambiguously specified using matching arguments.");
		} else if (matchingCalls.size() == 1) {
			return matchingCalls.get(0);
		} else {
			return null;
		}
	}

	private boolean match(MethodCall subject, MethodCall query) {
		// TODO: Debug
		boolean matches = true;
		for (int i = 0; i < query.getArgs().length; i++) {
			Object queryArg = query.getArgs()[i];
			Object subjectArg = subject.getArgs()[i];
			if (queryArg instanceof Matcher) {
				Matcher<?> matcher = (Matcher<?>) queryArg;
				matches &= matcher.matches(subjectArg);
			} else if (queryArg instanceof Predicate) {
				Predicate<?> predicate = (Predicate<?>) queryArg;
				matches &= predicateMatches(predicate, subjectArg);
			} else {
				if (subjectArg != null && queryArg != null && subjectArg.getClass().isArray()
						&& queryArg.getClass().isArray()) {
					matches &= Arrays.deepEquals((Object[]) queryArg, (Object[]) subjectArg);
				} else {
					matches &= queryArg == null ? queryArg == subjectArg : queryArg.equals(subjectArg);
				}
			}
		}
		return matches;
	}

	private <T> boolean predicateMatches(Predicate<?> predicate, T subject) {
		@SuppressWarnings("unchecked")
		Predicate<T> applicablePredicate = (Predicate<T>) predicate;
		return applicablePredicate.test(subject);
	}

	private boolean anyMatcherPresent(Object[] args) {
		for (Object arg : args) {
			if (arg instanceof Matcher || arg instanceof Predicate) {
				return true;
			}
		}
		return false;
	}

	private String getCommaSeparatedArgs(MethodCall methodCall) {
		Object[] args = methodCall.getArgs();
		if (args == null || args.length == 0) {
			return "";
		} else {
			return Arrays.stream(args).map(arg -> arg == null ? "null" : arg.toString())
					.collect(Collectors.joining(", "));
		}
	}

	public boolean verifyNoMoreMethodInvocations() {
		return verifyNoMoreMethodInvocations(true);
	}

	public boolean verifyNoMoreMethodInvocations(boolean printStackTrace) {
		if (calls.isEmpty()) {
			return true;
		} else {
			log.error("Calls remaining (that were not removed):{}{}", System.lineSeparator(), getNewlineSeperatedCalls(
					(Predicate<Entry<MethodCall, List<MethodCallInformation>>>) (entry) -> true, printStackTrace));
			return false;
		}
	}

	private String getNewlineSeperatedCalls(Predicate<Entry<MethodCall, List<MethodCallInformation>>> predicate,
			boolean printStackTrace) {
		if (calls.isEmpty()) {
			return "";
		} else {
			return calls.entrySet().stream().filter(predicate)
					.map(entry -> " * Method: " + entry.getKey().getMethod() + ", Args: ["
							+ getCommaSeparatedArgs(entry.getKey()) + "], Times invoked: " + entry.getValue().size()
							+ (printStackTrace
									? ", Stack traces:" + System.lineSeparator() + stackTracesAsString(entry.getValue())
									: "."))
					.collect(Collectors.joining(System.lineSeparator()));
		}
	}

	private String stackTracesAsString(List<MethodCallInformation> stackTraces) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < stackTraces.size(); i++) {
			StackTraceElement[] stackTrace = stackTraces.get(i).getStackTraceElements();
			String tracePrefix = (i < stackTraces.size() - 1 ? " |" : "  ");
			sb.append(" |").append("__[ StackTrace for method call[").append(i)
					.append("] (" + getInvocationCount(stackTraces.get(i).getMethodInvocationSequenceNo())
							+ " invocation on this mock): ]")
					.append(System.lineSeparator()).append(tracePrefix)
					.append(Arrays.stream(stackTrace).skip(2)
							.map(stackTraceElement -> "\t-> " + stackTraceElement.toString())
							.collect(Collectors.joining(System.lineSeparator() + tracePrefix)))
					.append(System.lineSeparator());
		}
		return sb.toString();
	}

	private String getInvocationCount(int methodInvocationSequenceNoStartingFromZero) {
		int methodInvocationSequenceNoStartingFromOne = methodInvocationSequenceNoStartingFromZero + 1;
		if (methodInvocationSequenceNoStartingFromOne % 10 == 1 && methodInvocationSequenceNoStartingFromOne != 11) {
			return methodInvocationSequenceNoStartingFromOne + "st";
		} else if (methodInvocationSequenceNoStartingFromOne % 10 == 2
				&& methodInvocationSequenceNoStartingFromOne != 12) {
			return methodInvocationSequenceNoStartingFromOne + "nd";
		} else if (methodInvocationSequenceNoStartingFromOne % 10 == 3
				&& methodInvocationSequenceNoStartingFromOne != 13) {
			return methodInvocationSequenceNoStartingFromOne + "rd";
		} else {
			return methodInvocationSequenceNoStartingFromOne + "th";
		}
	}

	public static void traceLogMethodCall() {
		log.trace("In method: " + Thread.currentThread().getStackTrace()[3] + ", called from: "
				+ Thread.currentThread().getStackTrace()[4]);
	}

	private void checkInitializedWithStringKeyClass() {
		if (keyClass != String.class) {
			throw new IllegalArgumentException("Please use an instance initialized with String.class as key class.");
		}
	}

	private void checkInitializedWithMethodKeyClass() {
		if (keyClass != Method.class) {
			throw new IllegalArgumentException("Please use an instance initialized with Method.class as key class.");
		}
	}

	public void reset() {
		calls.clear();
		sequentialCallNo = 0;
		sequentialCallVerificationNo = 0;
	}

}
