package com.semantica.pocketknife.methodrecorder;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import com.semantica.pocketknife.util.Assert;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;
import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class CodeGenerationMigrationSpikes {

	private static final Objenesis objenesis = new ObjenesisStd();
	private static final Map<Integer, Class<?>> instancesOf = new HashMap<>();

	public static <T> T getInstance(Class<T> clazz) {
		/*
		 * We not only create a new subclass only for Abstract classes and Interfaces,
		 * but also for concrete classes because otherwise two different instances (of
		 * exactly the requested class, created by Objenesis) returned from this method
		 * would equals(..). Now, all instances will be of a different subclass
		 * [enhancer.setUseCache(false)] and will not equals(..) as with the equals(..)
		 * method intercepted and implemented in the intercept(..) method.
		 */
		Class<?> requestedClass = clazz;
		Enhancer enhancer = new Enhancer();
		enhancer.setUseCache(false);
		enhancer.setSuperclass(clazz);
		enhancer.setCallbackType(MethodInterceptor.class);
		clazz = enhancer.createClass();
		Enhancer.registerCallbacks(clazz,
				new Callback[] { (MethodInterceptor) CodeGenerationMigrationSpikes::intercept });
		T newInstance = objenesis.newInstance(clazz);
		instancesOf.put(System.identityHashCode(newInstance), requestedClass);
		return newInstance;
	}

	private static Object intercept(Object obj, java.lang.reflect.Method method, Object[] args, MethodProxy proxy)
			throws Throwable {
		if (method.getName().equals("hashCode") && args.length == 0) {
			return System.identityHashCode(obj);
		} else if (method.getName().equals("toString") && args.length == 0) {
			return "Identifier dummy instance of class: " + instancesOf.get(System.identityHashCode(obj))
					+ ", hashCode: " + System.identityHashCode(obj);
		} else if (method.getName().equals("equals") && args.length == 1) {
			return System.identityHashCode(obj) == System.identityHashCode(args[0]);
		} else if (method.getName().equals("get") && args.length == 0) {
			return "From proxy.";
		} else {
			return null;
		}
	}

	class ClassUnderTest {
		public String get() {
			return "From real instance.";
		}

		public String get2() {
			return "From real instance (2)";
		}
	}

	@Test
	public void shouldReturnFromRealInstance() {
		ClassUnderTest myInstance = new ClassUnderTest();
		Assert.actual(myInstance.get()).equalsExpected("From real instance.");
	}

	@Test
	public void shouldReturnFromProxyInstance() {
		ClassUnderTest myInstance = getInstance(ClassUnderTest.class);
		Assert.actual(myInstance.get()).equalsExpected("From proxy.");
	}

	public static class SampleClass {

		public SampleClass() {
			super();
		}

		public String test() {
			return "foo";
		}
	}

	@Test
	public void shouldReturnFooBarFromCglib() {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(SampleClass.class);
		enhancer.setCallback(new MethodInterceptor() {
			@Override
			public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
				return proxy.invokeSuper(obj, args) + "bar";
			}
		});
		SampleClass proxy = (SampleClass) enhancer.create();
		Assertions.assertEquals("foobar", proxy.test());
	}

	public static class SampleClassInterceptor {
		public static String intercept(@SuperCall Callable<String> zuper) throws Exception {
			return zuper.call() + "bar";
		}
	}

	@Test
	public void shouldReturnFooBarFromByteBuddy() throws IllegalAccessException, InstantiationException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		SampleClass proxy = new ByteBuddy().subclass(SampleClass.class).method(ElementMatchers.named("test"))
				.intercept(MethodDelegation.to(SampleClassInterceptor.class)).make()
				.load(ClassLoader.getSystemClassLoader(),
						ClassLoadingStrategy.UsingLookup
								.of(MethodHandles.privateLookupIn(SampleClass.class, MethodHandles.lookup())))
				.getLoaded().getDeclaredConstructor().newInstance();
		Assertions.assertEquals("foobar", proxy.test());
	}

	public static class SampleGenericClassInterceptor {
		@RuntimeType
		public static Object intercept(@Origin Method method, @This Object self, @AllArguments Object[] args,
				@SuperCall Callable<String> zuper) throws Exception {
			return zuper.call() + "bar";
		}
	}

	@Test
	public void shouldReturnFooBarFromByteBuddyGeneric() throws IllegalAccessException, InstantiationException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		SampleClass proxy = new ByteBuddy().subclass(SampleClass.class).method(ElementMatchers.any())
				.intercept(MethodDelegation.to(SampleGenericClassInterceptor.class)).make()
				.load(ClassLoader.getSystemClassLoader(),
						ClassLoadingStrategy.UsingLookup
								.of(MethodHandles.privateLookupIn(SampleClass.class, MethodHandles.lookup())))
				.getLoaded().getDeclaredConstructor().newInstance();
		Assertions.assertEquals("foobar", proxy.test());
	}

	public class FilledGenericClassInterceptor {
		@RuntimeType
		public Object intercept(@Origin Method method, @This Object self, @AllArguments Object[] args,
				@SuperCall Callable<String> zuper) throws Exception {
			if (method.getName().equals("hashCode") && args.length == 0) {
				return System.identityHashCode(self);
			} else if (method.getName().equals("toString") && args.length == 0) {
				return "Identifier dummy instance of class: " + instancesOf.get(System.identityHashCode(self))
						+ ", hashCode: " + System.identityHashCode(self);
			} else if (method.getName().equals("equals") && args.length == 1) {
				return System.identityHashCode(self) == System.identityHashCode(args[0]);
			} else if (method.getName().equals("get") && args.length == 0) {
				return "From proxy.";
			} else if (method.getName().equals("get2") && args.length == 0) {
				return zuper.call() + " and from proxy!";
			} else {
				return null;
			}
		}
	}

	public static class Interceptor {
		@RuntimeType
		public static Object intercept(@Origin Method method, @This Object self, @AllArguments Object[] args,
				@SuperCall Callable<String> zuper) throws Exception {
			return null;
		}
	}

	public <T> T getInstanceFromByteBuddyWithInstanceInterceptor(Class<T> clazz) throws IllegalAccessException {
		/*
		 * We not only create a new subclass only for Abstract classes and Interfaces,
		 * but also for concrete classes because otherwise two different instances (of
		 * exactly the requested class, created by Objenesis) returned from this method
		 * would equals(..). Now, all instances will be of a different subclass
		 * [enhancer.setUseCache(false)] and will not equals(..) as with the equals(..)
		 * method intercepted and implemented in the intercept(..) method.
		 */
		Class<?> requestedClass = clazz;

		Class<? extends T> newClass = new ByteBuddy().subclass(clazz).method(ElementMatchers.any())
				.intercept(MethodDelegation.to(new FilledGenericClassInterceptor())).make()
				.load(ClassLoader.getSystemClassLoader(), ClassLoadingStrategy.UsingLookup
						.of(MethodHandles.privateLookupIn(clazz, MethodHandles.lookup())))
				.getLoaded();

		T newInstance = objenesis.newInstance(newClass);
		instancesOf.put(System.identityHashCode(newInstance), requestedClass);
		return newInstance;
	}

	@Test
	public void shouldReturnFromByteBuddyProxyInstance() throws IllegalAccessException {
		ClassUnderTest myInstance = getInstanceFromByteBuddyWithInstanceInterceptor(ClassUnderTest.class);
		Assert.actual(myInstance.get()).equalsExpected("From proxy.");
	}

	@Test
	public void shouldReturnFromByteBuddyDelegatedInstanceFromProxy() throws IllegalAccessException {
		ClassUnderTest myInstance = getInstanceFromByteBuddyWithInstanceInterceptor(ClassUnderTest.class);
		Assert.actual(myInstance.get2()).equalsExpected("From real instance (2) and from proxy!");
	}

	public <T> T getInstanceFromByteBuddyWithInstanceInterceptorDefinedInSamePackage(Class<T> clazz)
			throws IllegalAccessException {
		/*
		 * We not only create a new subclass only for Abstract classes and Interfaces,
		 * but also for concrete classes because otherwise two different instances (of
		 * exactly the requested class, created by Objenesis) returned from this method
		 * would equals(..). Now, all instances will be of a different subclass
		 * [enhancer.setUseCache(false)] and will not equals(..) as with the equals(..)
		 * method intercepted and implemented in the intercept(..) method.
		 */
		Class<?> requestedClass = clazz;

		Class<? extends T> newClass = new ByteBuddy().subclass(clazz)
				.name(this.getClass().getPackage().getName() + "." + clazz.getSimpleName() + "Proxy" + "_"
						+ UUID.randomUUID().toString().replaceAll("-", ""))
				.method(ElementMatchers.any()).intercept(MethodDelegation.to(new FilledGenericClassInterceptor()))
				.make()
				.load(ClassLoader.getSystemClassLoader(),
						ClassLoadingStrategy.UsingLookup
								.of(MethodHandles.privateLookupIn(this.getClass(), MethodHandles.lookup())))
				.getLoaded();

		T newInstance = objenesis.newInstance(newClass);
		instancesOf.put(System.identityHashCode(newInstance), requestedClass);
		return newInstance;
	}

	@Test
	public void shouldReturnFromByteBuddyProxyInstanceDefinedInSamePackage() throws IllegalAccessException {
		Object myInstance = getInstanceFromByteBuddyWithInstanceInterceptorDefinedInSamePackage(Object.class);
		assert myInstance.toString()
				.startsWith("Identifier dummy instance of class: class java.lang.Object, hashCode:");
		assert myInstance.getClass().toString()
				.startsWith("class com.semantica.pocketknife.methodrecorder.ObjectProxy_");
	}

	@Test
	public void test() {
		System.out.println(this.getClass().getPackage().getName());
	}

}
