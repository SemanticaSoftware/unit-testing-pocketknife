package com.semantica.pocketknife.methodrecorder;

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

class RandomValues {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RandomValues.class);
	private static final Random RANDOM = new Random();
	private static final Objenesis objenesis = new ObjenesisStd();
	private static final Map<Integer, Class<?>> instancesOf = new HashMap<>();

	/**
	 * This method returns a new identifier value (new instance or random value)
	 * matching the given class instance. This method returns a random numeric value
	 * for each of the numeric primitive and wrapper types. For boolean types it
	 * returns false. For reference types other than the numeric wrappers, it
	 * returns a new instance instantiated using the Objenesis library. For
	 * interfaces and abstract types, it generates a proxy instance from a subclass
	 * dynamically using cglib. For this proxy instance, the hashCode(), toString()
	 * and equals() methods are implemented.
	 *
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	static <T> T identifierValue(Class<T> clazz) {
		if (clazz.isArray()) {
			return (T) java.lang.reflect.Array.newInstance(clazz.getComponentType(), 1);
		} else if (clazz == Boolean.class || clazz == boolean.class) {
			return (T) Boolean.FALSE;
		} else if (clazz == Character.class || clazz == char.class) {
			return (T) (Character) (char) RANDOM.nextInt(Character.MAX_VALUE + 1);
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
				Enhancer.registerCallbacks(clazz, new Callback[] { (MethodInterceptor) RandomValues::intercept });
			}
			T newInstance = objenesis.newInstance(clazz);
			instancesOf.put(System.identityHashCode(newInstance), requestedClass);
			return newInstance;
		}
	}

	private static Object intercept(Object obj, java.lang.reflect.Method method, Object[] args, MethodProxy proxy)
			throws Throwable {
		if (method.getName().equals("hashCode") && method.getReturnType() == int.class && args.length == 0) {
			return System.identityHashCode(obj);
		} else if (method.getName().equals("toString") && method.getReturnType() == String.class && args.length == 0) {
			return "Identifier dummy instance of class: " + instancesOf.get(System.identityHashCode(obj))
					+ ", hashCode: " + System.identityHashCode(obj);
		} else if (method.getName().equals("equals") && method.getReturnType() == boolean.class && args.length == 1) {
			return System.identityHashCode(obj) == System.identityHashCode(args[0]);
		} else {
			return null;
		}
	}

}
