package com.semantica.pocketknife.mock.service;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

import com.semantica.pocketknife.calls.MethodCall;
import com.semantica.pocketknife.mock.InterceptionsStore;
import com.semantica.pocketknife.mock.MockUtils;
import com.semantica.pocketknife.mock.dto.QualifiedMethodCall;

public class InlineMockerInterceptionsStore implements InterceptionsStore {

	// key: the proxy (mock) instance
	private final Map<Object, Map<MethodCall<Method>, Queue<Object>>> allInterceptions = new HashMap<>();

	@Override
	public void addInterceptions(QualifiedMethodCall<Method> qualifiedMethodCall, List<Object> returnValues) {
		Object proxy = qualifiedMethodCall.getInvokedOnInstance();
		Map<MethodCall<Method>, Queue<Object>> interceptions = allInterceptions.get(proxy);
		if (interceptions == null) {
			interceptions = new HashMap<>();
			allInterceptions.put(proxy, interceptions);
		}
		Queue<Object> orderedReturnValues = interceptions.get(proxy);
		if (orderedReturnValues == null) {
			orderedReturnValues = new ArrayDeque<>(returnValues.size());
			interceptions.put(qualifiedMethodCall.getMethodCall(), orderedReturnValues);
		}
		for (Object returnValue : returnValues) {
			orderedReturnValues.add(returnValue);
		}
	}

	@Override
	public Optional<Object> matchExactMethodCallToStoredMatchingMethodCalls(
			QualifiedMethodCall<Method> qualifiedMethodCall) {
		Object proxy = qualifiedMethodCall.getInvokedOnInstance();
		Map<MethodCall<Method>, Queue<Object>> interceptions = allInterceptions.get(proxy);
		if (interceptions != null) {
			MethodCall<Method> matcherMethodCall = MockUtils
					.getStoredMatcherMethodCall(qualifiedMethodCall.getMethodCall(), interceptions.keySet());
			if (matcherMethodCall != null) {
				Object returnValue = interceptions.get(matcherMethodCall).poll();
				return Optional.ofNullable(returnValue);
			}
		}
		return Optional.empty();
	}

}
