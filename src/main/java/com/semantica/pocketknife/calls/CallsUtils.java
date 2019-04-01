package com.semantica.pocketknife.calls;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;

public class CallsUtils {

	public static <T> MethodCall<T> getStoredExactMethodCall(MethodCall<T> methodCallWithMatchingArguments,
			Set<MethodCall<T>> registeredExactMethodCalls) {
		final MethodCall<T> queryMethodCall = methodCallWithMatchingArguments;
		List<MethodCall<T>> matchingCalls = registeredExactMethodCalls.stream()
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

	public static <T> boolean match(MethodCall<T> withExactArguments, MethodCall<T> withMatchingArguments) {
		boolean matches = true;
		for (int i = 0; i < withMatchingArguments.getArgs().length; i++) {
			Object queryArg = withMatchingArguments.getArgs()[i];
			Object subjectArg = withExactArguments.getArgs()[i];
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

	private static <T> boolean predicateMatches(Predicate<?> predicate, T subject) {
		@SuppressWarnings("unchecked")
		Predicate<T> applicablePredicate = (Predicate<T>) predicate;
		return applicablePredicate.test(subject);
	}

}
