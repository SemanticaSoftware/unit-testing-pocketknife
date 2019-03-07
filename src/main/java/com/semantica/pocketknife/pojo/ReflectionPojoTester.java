package com.semantica.pocketknife.pojo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class ReflectionPojoTester {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReflectionPojoTester.class);
	private static final List<String> IGNORED_FIELD_NAMES = Arrays.asList("serialVersionUID");

	public static void testClassListForGettersSettersAndConstructors(List<Class<?>> pojoClassesToTest)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchFieldException, SecurityException {
		for (Class<?> c : pojoClassesToTest) {
			reflectionOnFieldsTest(c);
		}
	}

	public static void reflectionOnFieldsTest(Class<?> myClass) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchFieldException, SecurityException {
		List<GetterSetterPair> getterSetterPairs = getDefaultGettersAndSetters(myClass);
		for (GetterSetterPair getterSetterPair : getterSetterPairs) {
			log.debug("Getter: " + getterSetterPair.getter.toString() + ", setter: "
					+ getterSetterPair.setter.toString());
		}
		log.info("{}: Obtained default getter-setter pairs.", myClass);
		List<GetterSetterPair> customGetterSetterPairs = getCustomGettersAndSetters(myClass);
		for (GetterSetterPair getterSetterPair : customGetterSetterPairs) {
			log.debug("Getter: " + getterSetterPair.getter.toString() + ", setter: "
					+ getterSetterPair.setter.toString());
		}
		log.info("{}: Obtained custom getter-setter pairs.", myClass);
		Object myInstanceAfterSettersInvoked = testGettersAndSetters(getterSetterPairs, myClass, true);
		log.info("{}: Tested default getter-setter pairs.", myClass);
		testGettersAndSetters(customGetterSetterPairs, myClass, false);
		log.info("{}: Tested custom getter-setter pairs.", myClass);
		testConstructors(myClass);
		log.info("{}: Tested constructors.", myClass);
		Object otherInstanceAfterSettersInvoked = useSettersToCreateInstance(getterSetterPairs, myClass);
		testToStringHashcodeEqualsMethods(myClass, myInstanceAfterSettersInvoked, otherInstanceAfterSettersInvoked);
		log.info("{}: Checked toString(), equals() and hashCode() methods.", myClass);
	}

	private static List<GetterSetterPair> getDefaultGettersAndSetters(Class<?> myClass) {
		List<Field> fields = Arrays.stream(myClass.getDeclaredFields())
				.filter(field -> !IGNORED_FIELD_NAMES.contains(field.getName())).collect(Collectors.toList());
		String name;
		Method getter;
		Method setter;
		List<GetterSetterPair> getterSetterPairs = new ArrayList<GetterSetterPair>();
		for (Field field : fields) {
			name = field.getName();
			StringBuilder nameBuilder = new StringBuilder(name);
			nameBuilder.setCharAt(0, Character.toUpperCase(name.charAt(0)));
			try {
				if (field.getType().equals(boolean.class)) {
					nameBuilder.insert(0, "is");
					getter = myClass.getMethod(nameBuilder.toString());
					nameBuilder.replace(0, 2, "set");
					setter = myClass.getMethod(nameBuilder.toString(), field.getType());
					getterSetterPairs.add(new GetterSetterPair(getter, setter, field));
				} else {
					nameBuilder.insert(0, "get");
					getter = myClass.getMethod(nameBuilder.toString());
					nameBuilder.replace(0, 3, "set");
					setter = myClass.getMethod(nameBuilder.toString(), field.getType());
					getterSetterPairs.add(new GetterSetterPair(getter, setter, field));
				}
			} catch (NoSuchMethodException | SecurityException e) {
				log.error("Encountered an error while getting *default* getters and setters from class {}.", myClass,
						e);
			}
		}
		return getterSetterPairs;
	}

	private static List<GetterSetterPair> getCustomGettersAndSetters(Class<?> myClass) {
		Method[] methods = myClass.getDeclaredMethods();
		Field[] fields = myClass.getDeclaredFields();
		List<GetterSetterPair> customGetterSetterPairs = new ArrayList<GetterSetterPair>();
		List<String> fieldNames = new ArrayList<>();
		for (Field f : fields) {
			fieldNames.add(f.getName());
		}
		Method getter;
		Method setter;
		String methodName;
		for (Method m : methods) {
			methodName = m.getName();
			if (methodName.startsWith("set")) {
				try {
					setter = m;
					assert m.getParameterCount() == 1;
					StringBuilder getterBuilder = new StringBuilder(methodName);
					getterBuilder.delete(0, 3);
					String getterName;
					StringBuilder fieldBuilder = new StringBuilder(getterBuilder);
					fieldBuilder.setCharAt(0, Character.toLowerCase(fieldBuilder.charAt(0)));
					String expectedFieldName = fieldBuilder.toString();
					if (!fieldNames.contains(expectedFieldName)) {
						if (m.getParameterTypes()[0].equals(boolean.class)) {
							getterName = getterBuilder.insert(0, "is").toString();
							getter = myClass.getDeclaredMethod(getterName);
							customGetterSetterPairs.add(new GetterSetterPair(getter, setter, null));
						} else {
							getterName = getterBuilder.insert(0, "get").toString();
							getter = myClass.getDeclaredMethod(getterName);
							customGetterSetterPairs.add(new GetterSetterPair(getter, setter, null));
						}
					}
				} catch (NoSuchMethodException | SecurityException e) {
					log.error("Encountered an error while getting *custom* getters and setters from class {}.", myClass,
							e);
				}
			}
		}
		return customGetterSetterPairs;
	}

	private static Object testGettersAndSetters(List<GetterSetterPair> getterSetterPairs, Class<?> myClass,
			boolean compareWithFieldValues) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchFieldException, SecurityException {
		Object myInstance = myClass.newInstance();
		assert myInstance != null;
		for (GetterSetterPair getterSetterPair : getterSetterPairs) {
			Parameter setterParameter = getterSetterPair.setter.getParameters()[0];
			String setterParameterName = ReflectionUtils.getParameterName(setterParameter, getterSetterPair, null);
			Object parameter = TestValueProvider.getTestObjectForParameter(setterParameter, setterParameterName);
			getterSetterPair.setter.invoke(myInstance, parameter);
			Object returnValue = getterSetterPair.getter.invoke(myInstance);
			if (compareWithFieldValues) {
				boolean noDefaultValueFound = ReflectionUtils.isNoDefaultValue(getterSetterPair.correspondingField,
						myInstance);
				assert noDefaultValueFound;
				Object fieldObject = getterSetterPair.correspondingField.get(myInstance);
				if (parameter.equals(fieldObject)) {
					log.debug("Field value exactly matches the value that was passed to the setter for field: {"
							+ getterSetterPair.correspondingField + "} and getter: {" + getterSetterPair.getter + "}");
				} else {
					log.warn("Field value did not match the value that was passed to the setter for field: {"
							+ getterSetterPair.correspondingField + "} and getter: {" + getterSetterPair.getter + "}");
				}
			}
			assert parameter.equals(returnValue);
		}
		return myInstance;
	}

	private static void testConstructors(Class<?> myClass) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchFieldException, SecurityException {
		log.debug("Testing constructor for class {}...", myClass);
		Constructor<?>[] constructors = myClass.getDeclaredConstructors();
		Object newInstance = null;
		for (Constructor<?> constructor : constructors) {
			log.debug("Constructor: " + constructor.toGenericString());
			Parameter[] parameters = constructor.getParameters();

			Object[] testArguments = new Object[parameters.length];
			for (int i = 0; i < parameters.length; i++) {
				String fallBackParameterName = parameters[i].getName();
				String parameterName = ReflectionUtils.getParameterName(parameters[i], null, fallBackParameterName);
				log.debug("Creating test Object for constructor parameter: {}", parameters[i].toString());
				testArguments[i] = TestValueProvider.getTestObjectForParameter(parameters[i], parameterName);
			}
			newInstance = constructor.newInstance(testArguments);
			assert newInstance != null;
			if (parameters.length != 0) {
				assert ReflectionUtils.allFieldsInitialized(newInstance);
			} else {
				log.debug("Skipping no-arguments constructor...");
			}
		}
	}

	private static Object useSettersToCreateInstance(List<GetterSetterPair> getterSetterPairs, Class<?> myClass)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchFieldException, SecurityException {
		Object myInstance = myClass.newInstance();
		assert myInstance != null;
		for (GetterSetterPair getterSetterPair : getterSetterPairs) {
			Parameter setterParameter = getterSetterPair.setter.getParameters()[0];
			String setterParameterName = ReflectionUtils.getParameterName(setterParameter, getterSetterPair, null);
			Object parameter = TestValueProvider.getTestObjectForParameter(setterParameter, setterParameterName);
			getterSetterPair.setter.invoke(myInstance, parameter);
			Object returnValue = getterSetterPair.getter.invoke(myInstance);
		}
		return myInstance;
	}

	private static void testToStringHashcodeEqualsMethods(Class<?> myClass, Object myInstanceWithFieldsSet,
			Object otherInstanceAfterSettersInvoked) throws InstantiationException, IllegalAccessException {
		String expectedObjectString = ReflectionToStringBuilder.toString(myInstanceWithFieldsSet);
		String actualObjectString = myInstanceWithFieldsSet.toString();
		int expectedHashCode = HashCodeBuilder.reflectionHashCode(myInstanceWithFieldsSet);
		int actualHashCode = myInstanceWithFieldsSet.hashCode();
		boolean expectedEqualsResult = EqualsBuilder.reflectionEquals(myInstanceWithFieldsSet,
				otherInstanceAfterSettersInvoked);
		boolean actualEqualsResult = myInstanceWithFieldsSet.equals(otherInstanceAfterSettersInvoked);
		log.info(myInstanceWithFieldsSet.getClass().getName() + "'s toString(): " + actualObjectString);
		log.info(myInstanceWithFieldsSet.getClass().getName() + "'s hashCode(): " + actualHashCode);
		log.info(myInstanceWithFieldsSet.getClass().getName() + "'s equals("
				+ otherInstanceAfterSettersInvoked.getClass().getName() + "): " + actualEqualsResult);
		assert expectedObjectString.equals(actualObjectString);
		assert expectedHashCode == actualHashCode;
		assert expectedEqualsResult == actualEqualsResult;
		assert !myInstanceWithFieldsSet.equals(myClass.newInstance());
		assert !myInstanceWithFieldsSet.equals(null);
	}

}