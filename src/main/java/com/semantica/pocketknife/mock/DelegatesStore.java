package com.semantica.pocketknife.mock;

import java.lang.reflect.Method;
import java.util.Optional;

import com.semantica.pocketknife.mock.dto.QualifiedMethodCall;

public interface DelegatesStore {

	public <S> void register(Class<S> interfaze, S mock, S delegate);

	public Optional<Object> executeDelegate(QualifiedMethodCall<Method> qualifiedMethodCall);
}