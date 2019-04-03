package com.semantica.pocketknife.mock.service.support.components;

import java.util.Optional;

import com.semantica.pocketknife.methodrecorder.MethodRecorder;

public class DynamicMockingMethodRecorder<T> extends MethodRecorder<T> {

	public DynamicMockingMethodRecorder(Class<T> recordedClass) {
		super(recordedClass);
	}

	public <S> S storeMatcherWithCastedIdInstanceOfTypeArgumentAsKey(Object matcher, Class<S> clazz,
			Optional<Integer> argumentNumber, Object identifierValue) {
		S typedIdentifierValue = wrapperClassForPrimitiveClasses(clazz).cast(identifierValue);
		return super.storeMatcherWithIdInstanceOfTypeArgumentAsKey(matcher, clazz, argumentNumber,
				typedIdentifierValue);
	}

	@SuppressWarnings("unchecked")
	private <S> Class<S> wrapperClassForPrimitiveClasses(Class<S> clazz) {
		if (clazz == boolean.class) {
			return (Class<S>) Boolean.class;
		} else if (clazz == byte.class) {
			return (Class<S>) Byte.class;
		} else if (clazz == short.class) {
			return (Class<S>) Short.class;
		} else if (clazz == char.class) {
			return (Class<S>) Character.class;
		} else if (clazz == int.class) {
			return (Class<S>) Integer.class;
		} else if (clazz == long.class) {
			return (Class<S>) Long.class;
		} else if (clazz == float.class) {
			return (Class<S>) Float.class;
		} else if (clazz == double.class) {
			return (Class<S>) Double.class;
		} else {
			return clazz;
		}
	}

}
