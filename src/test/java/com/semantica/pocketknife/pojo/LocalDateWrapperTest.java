package com.semantica.pocketknife.pojo;

import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

public class LocalDateWrapperTest {

	@Test
	public void test() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchFieldException, SecurityException {
		ReflectionPojoTester.reflectionOnFieldsTest(LocalDateWrapper.class);
	}

}
