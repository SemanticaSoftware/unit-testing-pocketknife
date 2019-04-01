package com.semantica.pocketknife.mock;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.semantica.pocketknife.calls.CallsUtils;
import com.semantica.pocketknife.calls.MethodCall;

public class MockUtils {

	public static <T> MethodCall<T> getStoredMatcherMethodCall(MethodCall<T> interceptedExactQueryMethodCall,
			Set<MethodCall<T>> registeredMethodCallsWithMatchingArguments) {
		final MethodCall<T> exactQueryMethodCall = interceptedExactQueryMethodCall;
		List<MethodCall<T>> matchingRegisteredMatcherCalls = registeredMethodCallsWithMatchingArguments.stream()
				.filter(registeredMatcherCall -> registeredMatcherCall.getMethod()
						.equals(exactQueryMethodCall.getMethod()))
				.filter(registeredMatcherCall -> registeredMatcherCall.getArgs().length == exactQueryMethodCall
						.getArgs().length)
				.filter(registeredMatcherCall -> CallsUtils.match(exactQueryMethodCall, registeredMatcherCall))
				.collect(Collectors.toList());
		if (matchingRegisteredMatcherCalls.size() > 1) {
			String ambiguouslyDefinedInterceptionMethodCalls = matchingRegisteredMatcherCalls.stream().map(
					ambiguouslyDefinedInterceptionMethodCall -> ambiguouslyDefinedInterceptionMethodCall.toString())
					.collect(Collectors.joining(", "));
			throw new IllegalArgumentException(String.format(
					"The mock interceptions are ambiguously specified using matching arguments. Intercepted method call \"%s\" matches the interception method calls:\"%s\".",
					interceptedExactQueryMethodCall, ambiguouslyDefinedInterceptionMethodCalls));
		} else if (matchingRegisteredMatcherCalls.size() == 1) {
			return matchingRegisteredMatcherCalls.get(0);
		} else {
			return null;
		}
	}

}
