package com.semantica.pocketknife.methodrecorder.dynamicproxies;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import com.semantica.pocketknife.methodrecorder.FatalTestException;

import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

public class ClassLoadingStrategyFinder<T> {

	private final Class<T> classInTargetPackage;

	/**
	 *
	 * @param classInTargetPackage The class in the current module whose package the
	 *                             dynamic proxies will be defined in.
	 */
	public ClassLoadingStrategyFinder(Class<T> classInTargetPackage) {
		super();
		this.classInTargetPackage = classInTargetPackage;
	}

	/**
	 * Method returning the appropriate proxy class loading strategy to for the
	 * current JVM. This method invokes a call site sensitive method
	 * (MethodHandles::lookup), therefore, per module that invokes this method, this
	 * method should be defined inside the same module.
	 *
	 * @return The appropriate class loading strategy for Byte Buddy.
	 */
	public ClassLoadingStrategy<ClassLoader> getClassLoadingStrategyToDefineClassInSamePackageAsClassInTargetPackage() {
		if (ClassInjector.UsingLookup.isAvailable()) { // Java version >= 9
			Object privateLookup;
			try {
				Class<?> methodHandles = Class.forName("java.lang.invoke.MethodHandles");
				Object lookup = methodHandles.getMethod("lookup").invoke(null);
				Method privateLookupIn = methodHandles.getMethod("privateLookupIn", Class.class,
						Class.forName("java.lang.invoke.MethodHandles$Lookup"));
				privateLookup = privateLookupIn.invoke(null, classInTargetPackage, lookup);
			} catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new FatalTestException("Unable to construct private lookup", e);
			}
			return ClassLoadingStrategy.UsingLookup.of(privateLookup);
		} else if (ClassInjector.UsingReflection.isAvailable()) { // Java version <= 1.8
			return ClassLoadingStrategy.Default.INJECTION;
		} else {
			throw new IllegalStateException("No code generation strategy available.");
		}
	}

	public String getTargetClassNameUniqueForTargetClassMatchingStrategy(Class<?> targetClass,
			String proxyTypeIdentifier) {
		return getTargetClassNameMatchingStrategy(targetClass, proxyTypeIdentifier) + "_"
				+ UUID.randomUUID().toString().replaceAll("-", "");
	}

	public String getTargetClassNameMatchingStrategy(Class<?> targetClass, String proxyTypeIdentifier) {
		return classInTargetPackage.getPackage().getName() + "." + targetClass.getSimpleName() + proxyTypeIdentifier;
	}

	public ClassLoader getClassLoader() {
		return classInTargetPackage.getClassLoader();
	}

}
