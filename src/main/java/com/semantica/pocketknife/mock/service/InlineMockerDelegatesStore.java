package com.semantica.pocketknife.mock.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.semantica.pocketknife.calls.MethodCall;
import com.semantica.pocketknife.mock.DelegatesStore;
import com.semantica.pocketknife.mock.InlineMocker;
import com.semantica.pocketknife.mock.dto.QualifiedMethodCall;
import com.semantica.pocketknife.util.Tuple;

public class InlineMockerDelegatesStore implements DelegatesStore {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InlineMockerDelegatesStore.class);

	// key: tuple of class of delegated interface and mock instance
	private final Map<Tuple<Class<?>, ?>, Object> delegates = new HashMap<>();

	@Override
	public <S> void register(Class<S> interfaze, S mock, S delegate) {
		if (interfaze.isInterface() && interfaze.isAssignableFrom(mock.getClass())
				&& interfaze.isAssignableFrom(delegate.getClass())) {
			Tuple<Class<?>, ?> key = new Tuple<>(interfaze, mock);
			delegates.put(key, delegate);
		} else {
			throw new IllegalArgumentException("Both mock and delegate should be a subtype of the given interface.");
		}

	}

	/*
	 * Both the mock and delegate are instances of the interface (see delegate(..)
	 * method). We only want to delegate methods in the interface. If the current
	 * method is in the interface, the declaring class is the interface. Therefore,
	 * we request a tuple key with this interface and proxy, and invoke the delegate
	 * if it has been set (not null).
	 *
	 */
	@Override
	public Optional<Object> executeDelegate(QualifiedMethodCall<Method> qualifiedMethodCall) {
		MethodCall<Method> methodCall = qualifiedMethodCall.getMethodCall();
		Object proxy = qualifiedMethodCall.getInvokedOnInstance();
		Object delegate = delegates.get(new Tuple<>(methodCall.getMethod().getDeclaringClass(), proxy));
		if (delegate != null) {
			try {
				return Optional.ofNullable(methodCall.getMethod().invoke(delegate, methodCall.getArgs()));
			} catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
				log.error("Cannot invoke delegate method {}.", methodCall.getMethod(), e.getCause());
				throw new RuntimeException(e);
			}
		}
		return Optional.empty();
	}

}
