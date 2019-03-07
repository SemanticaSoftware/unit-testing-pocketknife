package com.semantica.pocketknife;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

@SuppressWarnings("unchecked")
public class Primitives {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Primitives.class);
	private static final Map<Class<?>, Object> PRIMITIVE_OR_WRAPPER_DEFAULT_VALUES = new HashMap<Class<?>, Object>();
	private static final Random RANDOM = new Random();
	private static final Objenesis objenesis = new ObjenesisStd();
	private static final Map<Integer, Class<?>> instancesOf = new HashMap<>();

	public static <T> T defaultValue(Class<T> primitiveOrWrapperType) {
		return (T) PRIMITIVE_OR_WRAPPER_DEFAULT_VALUES.get(primitiveOrWrapperType);
	}

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

	public static <T> T identifierValue(Class<T> clazz) {
		if (clazz.isArray()) {
			return (T) java.lang.reflect.Array.newInstance(clazz.getComponentType(), 1);
		} else if (clazz == Boolean.class || clazz == boolean.class) {
			return (T) Boolean.FALSE;
		} else if (clazz == Character.class || clazz == char.class) {
			return (T) (Integer) RANDOM.nextInt(Character.MAX_VALUE + 1);
		} else if (clazz == Byte.class || clazz == byte.class) {
			return (T) (Byte) (byte) (RANDOM.nextInt(Math.abs(Byte.MIN_VALUE) + Byte.MAX_VALUE + 1)
					- Math.abs(Byte.MIN_VALUE));
		} else if (clazz == Short.class || clazz == short.class) {
			return (T) (Short) (short) (RANDOM.nextInt(Math.abs(Short.MIN_VALUE) + Short.MAX_VALUE + 1)
					- Math.abs(Short.MIN_VALUE));
		} else if (clazz == Integer.class || clazz == int.class) {
			return (T) (Integer) (RANDOM.nextInt());
		} else if (clazz == Long.class || clazz == long.class) {
			return (T) (Long) (RANDOM.nextLong());
		} else if (clazz == Float.class || clazz == float.class) {
			return (T) (Float) (RANDOM.nextFloat());
		} else if (clazz == Double.class || clazz == double.class) {
			return (T) (Double) (RANDOM.nextDouble());
		} else {
			Class<?> requestedClass = clazz;
			if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
				Enhancer enhancer = new Enhancer();
				enhancer.setUseCache(false);
				enhancer.setSuperclass(clazz);
				enhancer.setCallbackType(MethodInterceptor.class);
				clazz = enhancer.createClass();
				Enhancer.registerCallbacks(clazz, new Callback[] { (MethodInterceptor) Primitives::intercept });
			}
			T newInstance = objenesis.newInstance(clazz);
			instancesOf.put(System.identityHashCode(newInstance), requestedClass);
			return newInstance;
		}
	}

	public static Object intercept(Object obj, java.lang.reflect.Method method, Object[] args, MethodProxy proxy)
			throws Throwable {
		if (method.getName().equals("hashCode") && method.getReturnType() == int.class) {
			return System.identityHashCode(obj);
		} else if (method.getName().equals("toString") && method.getReturnType() == String.class) {
			return "Identifier dummy instance of class: " + instancesOf.get(System.identityHashCode(obj))
					+ ", hashCode: " + System.identityHashCode(obj);
		} else if (method.getName().equals("equals") && method.getReturnType() == boolean.class) {
			return System.identityHashCode(obj) == System.identityHashCode(args[0]);
		} else {
			return null;
		}
	}
}
