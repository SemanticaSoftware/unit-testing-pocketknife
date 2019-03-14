package com.semantica.pocketknife.methodrecorder;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
class DefaultValues {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultValues.class);
	private static final Map<Class<?>, Object> PRIMITIVE_OR_WRAPPER_DEFAULT_VALUES = new HashMap<Class<?>, Object>();
	static {
		PRIMITIVE_OR_WRAPPER_DEFAULT_VALUES.put(Boolean.class, false);
		PRIMITIVE_OR_WRAPPER_DEFAULT_VALUES.put(Character.class, '\u0000');
		PRIMITIVE_OR_WRAPPER_DEFAULT_VALUES.put(Byte.class, (byte) 0);
		PRIMITIVE_OR_WRAPPER_DEFAULT_VALUES.put(Short.class, (short) 0);
		PRIMITIVE_OR_WRAPPER_DEFAULT_VALUES.put(Integer.class, 0);
		PRIMITIVE_OR_WRAPPER_DEFAULT_VALUES.put(Long.class, 0L);
		PRIMITIVE_OR_WRAPPER_DEFAULT_VALUES.put(Float.class, 0F);
		PRIMITIVE_OR_WRAPPER_DEFAULT_VALUES.put(Double.class, 0D);

		PRIMITIVE_OR_WRAPPER_DEFAULT_VALUES.put(boolean.class, false);
		PRIMITIVE_OR_WRAPPER_DEFAULT_VALUES.put(char.class, '\u0000');
		PRIMITIVE_OR_WRAPPER_DEFAULT_VALUES.put(byte.class, (byte) 0);
		PRIMITIVE_OR_WRAPPER_DEFAULT_VALUES.put(short.class, (short) 0);
		PRIMITIVE_OR_WRAPPER_DEFAULT_VALUES.put(int.class, 0);
		PRIMITIVE_OR_WRAPPER_DEFAULT_VALUES.put(long.class, 0L);
		PRIMITIVE_OR_WRAPPER_DEFAULT_VALUES.put(float.class, 0F);
		PRIMITIVE_OR_WRAPPER_DEFAULT_VALUES.put(double.class, 0D);
	}

	/**
	 * This method will return the Java default value for the given primitive or
	 * wrapper type. For any other class (including void.class), it returns null,
	 * which is the Java default value for reference types.
	 *
	 * @param primitiveOrWrapperType
	 * @return The default value for the {@code primitiveOrWrapperType}, null
	 *         otherwise.
	 */
	static <T> T defaultValue(Class<T> primitiveOrWrapperType) {
		return (T) PRIMITIVE_OR_WRAPPER_DEFAULT_VALUES.get(primitiveOrWrapperType);

	}

}
