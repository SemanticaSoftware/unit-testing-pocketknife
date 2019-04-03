package com.semantica.pocketknife.mock;

import java.lang.reflect.Method;
import java.util.Optional;

import com.semantica.pocketknife.mock.dto.QualifiedMethodCall;

public interface ExactToMatchingMethodConverter {

	public <S> void register(Class<S> clazz, S proxy);

	public QualifiedMethodCall<Method> convert(QualifiedMethodCall<Method> qualifiedMethodCall);

	public <T> void storeMatcherCapture(Object matcher, Class<T> clazz, Optional<Integer> argumentNumber,
			T wiringIdentity);

}