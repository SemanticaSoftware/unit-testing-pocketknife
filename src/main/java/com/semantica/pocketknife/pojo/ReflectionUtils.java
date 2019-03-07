package com.semantica.pocketknife.pojo;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

public class ReflectionUtils {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReflectionUtils.class);

	public static String getParameterName(Parameter parameter, GetterSetterPair getterSetterPair,
			String fallBackParameterName) {
		String parameterName;
		if (parameter.isNamePresent()) {
			parameterName = parameter.getName();
		}
		/* When parameter names are not compiled-in to class files: */
		else if (getterSetterPair != null && getterSetterPair.correspondingField != null) {
			// field name should be equal to parameter name for getters and setters for
			// standard POJOs
			parameterName = getterSetterPair.correspondingField.getName();
		} else if (fallBackParameterName != null) {
			// The parameter index is serialized only correctly in the context of the
			// patameter array.
			parameterName = fallBackParameterName;
		} else {
			parameterName = parameter.getName();
		}
		return parameterName;
	}

	public static boolean allFieldsInitialized(Object newInstance)
			throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		Field[] fields = newInstance.getClass().getDeclaredFields();
		for (Field field : fields) {
			boolean noDefaultValueFound = isNoDefaultValue(field, newInstance);
			if (noDefaultValueFound) {
				continue;
			} else {
				return false;
			}
		}
		return true;
	}

	public static boolean isNoDefaultValue(Field field, Object enclosingClassInstance)
			throws IllegalArgumentException, IllegalAccessException {
		field.setAccessible(true);
		log.debug("Checking field for default value: {}", field.toGenericString());
		Class<?> fieldType = field.getType();
		Object fieldObject = field.get(enclosingClassInstance);
		if ((fieldType == boolean.class || fieldType == Boolean.class) && Boolean.FALSE.equals(fieldObject)) {
			return false; // fail
		} else if (fieldType.isPrimitive() && fieldType != boolean.class && ((Number) fieldObject).doubleValue() == 0) {
			return false; // fail
		} else if (!fieldType.isPrimitive() && fieldObject == null) {
			return false; // fail
		} else {
			return true;
		}
	}

}
