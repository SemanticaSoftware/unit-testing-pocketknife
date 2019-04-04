package com.semantica.pocketknife.methodrecorder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import org.hamcrest.Matcher;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import com.semantica.pocketknife.methodrecorder.dynamicproxies.ClassLoadingStrategyFinder;
import com.semantica.pocketknife.methodrecorder.dynamicproxies.Dummy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.TypeCache;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * Class that generates Random or "Identifier" values that are used by
 * {@link MethodRecorder} as an identifier value for matching arguments
 * ({@link Matcher} or {@link Predicate}. The matching argument is captured by
 * {@link MethodRecorder#storeAndCreateIdInstanceOfTypeArgument(Matcher, Class)}
 * or
 * {@link MethodRecorder#storeAndCreateIdInstanceOfTypeArgument(Predicate, Class)})
 * and temporarily stored in the {@link MethodRecorder} object, while the
 * identifier value is returned and used as argument to the method call on
 * {@link MethodRecorder}'s proxy. When this call with its arguments are parsed,
 * the matchers are substituted at the positions where the corresponding
 * identifier values are found.
 *
 * One important exception is that this class does not generate random values
 * for {@code boolean} and {@link Boolean} types. It just returns {@code false}
 * by default. This is because there simply is no gained benefit from an
 * identifier value that would be ambiguous in 50% of cases. It is more useful
 * to know that a {@link Boolean} matcher is defined ambiguously right away.
 *
 * Also, for reference types, not a random value but an identifying value is
 * generated. A new instance is created that will only equals(..) to another
 * instance if both are the same instance.
 *
 * @author A. Haanstra
 *
 */
public class RandomIdentifierValues {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RandomIdentifierValues.class);
	private static final Random RANDOM = new Random();
	private static final Objenesis OBJENESIS = new ObjenesisStd();
	private static final Map<Integer, Class<?>> INSTANCES_OF = new HashMap<>();
	private static final CallHandler CALL_HANDLER = new CallHandler();
	private static final TypeCache<Class<?>> TYPE_CACHE = new TypeCache<>(TypeCache.Sort.SOFT);

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
	 * @throws IllegalAccessException
	 */
	@SuppressWarnings("unchecked")
	public static <T> T identifierValue(Class<T> clazz) {
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
			/*
			 * We not only create a new subclass only for Abstract classes and Interfaces,
			 * but also for concrete classes because otherwise two different instances (of
			 * exactly the requested class, created by Objenesis) returned from this method
			 * would equals(..). Now, all instances will be of a different subclass
			 * [enhancer.setUseCache(false)] and will not equals(..) as with the equals(..)
			 * method intercepted and implemented in the intercept(..) method.
			 */
			Class<?> requestedClass = clazz;
			ClassLoadingStrategyFinder<Dummy> strategyFinder = new ClassLoadingStrategyFinder<>(Dummy.class);
			ClassLoadingStrategy<ClassLoader> strategy = strategyFinder
					.getClassLoadingStrategyToDefineClassInSamePackageAsClassInTargetPackage();
			Callable<Class<?>> proxyInstantiator = () -> new ByteBuddy().subclass(clazz)
					.name(strategyFinder.getTargetClassNameMatchingStrategy(clazz, "IdentifyingProxy"))
					.method(ElementMatchers.any()).intercept(InvocationHandlerAdapter.of(CALL_HANDLER)).make()
					.load(strategyFinder.getClassLoader(), strategy).getLoaded();
			Class<? extends T> newClass = (Class<? extends T>) TYPE_CACHE.findOrInsert(strategyFinder.getClassLoader(),
					requestedClass, proxyInstantiator);
			T newInstance = OBJENESIS.newInstance(newClass);
			INSTANCES_OF.put(System.identityHashCode(newInstance), requestedClass);
			return newInstance;
		}
	}

	private static class CallHandler implements InvocationHandler {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("hashCode") && args.length == 0) {
				return System.identityHashCode(proxy);
			} else if (method.getName().equals("toString") && args.length == 0) {
				return "Identifier dummy instance of class: " + INSTANCES_OF.get(System.identityHashCode(proxy))
						+ ", hashCode: " + System.identityHashCode(proxy);
			} else if (method.getName().equals("equals") && args.length == 1
					&& method.getParameterTypes()[0].equals(Object.class)) {
				return System.identityHashCode(proxy) == System.identityHashCode(args[0]);
			} else {
				return null;
			}
		}
	}

}
