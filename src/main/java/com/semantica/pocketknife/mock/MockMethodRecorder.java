package com.semantica.pocketknife.mock;

import java.util.Optional;

import com.semantica.pocketknife.methodrecorder.MethodRecorder;

public class MockMethodRecorder<T> extends MethodRecorder<T> {

	public MockMethodRecorder(Class<T> recordedClass) {
		super(recordedClass);
	}

	protected <S> S storeAndCreateIdInstanceOfTypeArgument(Object matcher, Class<S> clazz,
			Optional<Integer> argumentNumber) {
		return super.storeAndCreateIdInstanceOfTypeArgument(matcher, clazz, argumentNumber);
	}

}
