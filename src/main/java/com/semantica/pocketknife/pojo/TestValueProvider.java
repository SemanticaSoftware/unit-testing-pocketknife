package com.semantica.pocketknife.pojo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class TestValueProvider {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestValueProvider.class);

	private static final Map<Class<?>, Field> PREDEFINED_TEST_OBJECTS_FIELDS = Arrays
			.stream(PredefinedTestObjects.class.getDeclaredFields())
			.collect(Collectors.toMap(field -> field.getType(), Function.identity()));

	public static Object getTestObjectForParameter(Parameter parameter, String parameterName)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchFieldException, SecurityException {
		return getTestValueForType(parameter.getType(), parameterName);
	}

	/*
	 * Work in progress: not all types implemented yet!
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getTestValueForType(Class<? extends T> clazz, String variableName)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchFieldException, SecurityException {
		String simpleTypeName = clazz.getSimpleName();
		switch (simpleTypeName) {
		case "String":
			return (T) getTestValueForFieldName(variableName);
		case "long": // long.class instanceof Class<Integer>
			return (T) (Long) (long) getIntegerHashCodeForFieldName(variableName);
		case "int": // int.class instanceof Class<Integer>
			return (T) (Integer) getIntegerHashCodeForFieldName(variableName);
		case "boolean":
			return (T) Boolean.TRUE;
		case "List":
			return (T) Arrays.asList("test", variableName);
		default:
			if (clazz.isEnum()) {
				log.debug("Using first enum constant for enum: {}", clazz.getName());
				return clazz.getEnumConstants()[0];
			} else {
				log.debug("Trying to construct object for class: {}", clazz.getName());
				try {
					return getTestObjectForPojoClass(clazz, false);
				} catch (NoConstructorFoundException e) {
					log.debug("No accessible constructor found for class {}", clazz);
					try {
						return getPredefinedTestObjectForClass(clazz);
					} catch (ObjectNotConstructedException e1) {
						return getTestObjectForPojoClass(clazz, true);
					}
				}
			}
		}
	}

	private static String getTestValueForFieldName(String name) {
		return "test" + name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	private static int getIntegerHashCodeForFieldName(String name) {
		return ("test" + name.substring(0, 1).toUpperCase() + name.substring(1)).hashCode();
	}

	private static <T> T getTestObjectForPojoClass(Class<? extends T> clazz, boolean forceConstructorAccessible)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchFieldException, SecurityException {
		Constructor<?> constructorWithMostParameters = Arrays.stream(clazz.getDeclaredConstructors())
				.filter(c -> forceConstructorAccessible || c.isAccessible())
				.max((c1, c2) -> (int) Math.signum(c1.getParameterCount() - c2.getParameterCount())).orElseThrow(
						() -> new NoConstructorFoundException("No constructor found for class " + clazz.getName()));
		log.debug("Constructor with most parameters: {}", constructorWithMostParameters.toGenericString());
		Parameter[] parameters = constructorWithMostParameters.getParameters();
		Object[] testArguments = new Object[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			String fallBackParameterName = parameters[i].getName();
			String parameterName = ReflectionUtils.getParameterName(parameters[i], null, fallBackParameterName);
			log.debug("Creating test Object for constructor parameter: {}", parameters[i].toString());
			testArguments[i] = getTestObjectForParameter(parameters[i], parameterName);
		}
		constructorWithMostParameters.setAccessible(forceConstructorAccessible);
		@SuppressWarnings("unchecked")
		T newInstance = (T) constructorWithMostParameters.newInstance(testArguments);
		assert newInstance != null;
		if (parameters.length != 0) {
			assert ReflectionUtils.allFieldsInitialized(newInstance);
		} else {
			log.debug("Skipping no-arguments constructor...");
		}
		return newInstance;
	}

	@SuppressWarnings("unchecked")
	private static <T> T getPredefinedTestObjectForClass(Class<? extends T> clazz)
			throws IllegalArgumentException, IllegalAccessException, ObjectNotConstructedException {
		Field fieldWithTestObject = PREDEFINED_TEST_OBJECTS_FIELDS.get(clazz);
		if (fieldWithTestObject == null) {
			throw new ObjectNotConstructedException("No predefined test object found.");
		} else {
			return (T) fieldWithTestObject.get(null);
		}
	}

}
