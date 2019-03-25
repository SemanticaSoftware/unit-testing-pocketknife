package com.semantica.pocketknife.calls;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;

import com.semantica.pocketknife.util.TestUtils;

/**
 * This class contains all common behaviour and data of the
 * {@link CallsRegistry} and {@linkplain StrictCallsRegistry} classes.
 *
 * @author A. Haanstra
 *
 * @param <T>
 */
abstract class AbstractCallsRegistry<T> {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Calls.class);
	protected final Class<T> keyClass;
	protected Map<MethodCall<T>, List<MethodCallInformation>> calls = new HashMap<>();
	protected int sequentialCallNo = 0;

	/**
	 * Creates a Calls registry that is initialized to the given key class.
	 *
	 * @param methodClass Determines the class that will be used to store methods.
	 *                    Allowed values are String.class or Method.class
	 */
	protected AbstractCallsRegistry(Class<T> methodClass) {
		super();
		if (methodClass != String.class && methodClass != Method.class) {
			throw new IllegalArgumentException(
					"Unsupported method class used. Use either String.class or Method.class.");
		}
		this.keyClass = methodClass;
	}

	public void registerCall(Object... args) {
		requireNonNull(args);
		if (keyClass == String.class) {
			TestUtils.traceLogMethodCall(1);
			@SuppressWarnings("unchecked")
			T methodName = (T) TestUtils.getMethodName(1);
			MethodCall<T> methodCall = new MethodCall<>(methodName, args);
			addStackTraceToCalls(methodCall, Thread.currentThread().getStackTrace());
		} else {
			throw new UnsupportedOperationException(
					"Please use an instance initialized with String.class as key class.");
		}
	}

	public void registerCall(T method, Object... args) {
		requireNonNull(args);
		MethodCall<T> methodCall = new MethodCall<>(method, args);
		addStackTraceToCalls(methodCall, Thread.currentThread().getStackTrace());
	}

	public void registerCall(MethodCall<T> methodCall) {
		requireNonNull(methodCall.getArgs());
		addStackTraceToCalls(methodCall, Thread.currentThread().getStackTrace());
	}

	private void requireNonNull(Object[] args) {
		Objects.requireNonNull(args,
				"When a call is invoked without arguments, please use a zero-length args array (new Object[0]) instead of null.");
	}

	private void addStackTraceToCalls(MethodCall<T> methodCall, StackTraceElement[] stackTrace) {
		List<MethodCallInformation> stackTraces = calls.get(methodCall);
		if (stackTraces == null) {
			stackTraces = new ArrayList<>();
		}
		stackTraces.add(new MethodCallInformation(stackTrace, sequentialCallNo++));
		calls.put(methodCall, stackTraces);
	}

	protected MethodCall<T> getStoredExactMethodCall(MethodCall<T> methodCall) {
		Set<MethodCall<T>> registeredMethodCalls = calls.keySet();
		final MethodCall<T> queryMethodCall = methodCall;
		List<MethodCall<T>> matchingCalls = registeredMethodCalls.stream()
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

	private boolean match(MethodCall<T> subject, MethodCall<T> query) {
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

	private <S> boolean predicateMatches(Predicate<?> predicate, S subject) {
		@SuppressWarnings("unchecked")
		Predicate<S> applicablePredicate = (Predicate<S>) predicate;
		return applicablePredicate.test(subject);
	}

	public boolean verifyNoMoreMethodInvocations() {
		return verifyNoMoreMethodInvocations(true);
	}

	public boolean verifyNoMoreMethodInvocations(boolean printStackTrace) {
		if (calls.isEmpty()) {
			return true;
		} else {
			log.error("Calls remaining (that were not removed):{}{}", System.lineSeparator(), getNewlineSeperatedCalls(
					(Predicate<Entry<MethodCall<T>, List<MethodCallInformation>>>) (entry) -> true, printStackTrace));
			return false;
		}
	}

	protected String getNewlineSeperatedCalls(Predicate<Entry<MethodCall<T>, List<MethodCallInformation>>> predicate,
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

	protected String getCommaSeparatedArgs(MethodCall<T> methodCall) {
		Object[] args = methodCall.getArgs();
		if (args == null || args.length == 0) {
			return "";
		} else {
			return Arrays.stream(args).map(arg -> arg == null ? "null" : arg.toString())
					.collect(Collectors.joining(", "));
		}
	}

	private String stackTracesAsString(List<MethodCallInformation> stackTraces) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < stackTraces.size(); i++) {
			StackTraceElement[] stackTrace = stackTraces.get(i).getStackTraceElements();
			String tracePrefix = (i < stackTraces.size() - 1 ? " |" : "  ");
			sb.append(" |").append("__[ StackTrace for method call[").append(i)
					.append("] (" + TestUtils.getOrdinal(stackTraces.get(i).getMethodInvocationSequenceNo() + 1)
							+ " invocation on this mock): ]")
					.append(System.lineSeparator()).append(tracePrefix)
					.append(Arrays.stream(stackTrace).skip(2)
							.map(stackTraceElement -> "\t-> " + stackTraceElement.toString())
							.collect(Collectors.joining(System.lineSeparator() + tracePrefix)))
					.append(System.lineSeparator());
		}
		return sb.toString();
	}

	public void reset() {
		calls.clear();
		sequentialCallNo = 0;
	}

}
