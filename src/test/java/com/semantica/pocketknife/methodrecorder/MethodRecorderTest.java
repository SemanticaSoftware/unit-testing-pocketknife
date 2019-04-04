package com.semantica.pocketknife.methodrecorder;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.function.Predicate;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.semantica.pocketknife.calls.MethodCall;
import com.semantica.pocketknife.methodrecorder.AmbiguousArgumentsUtil.AmbiguouslyDefinedMatchersException;

public class MethodRecorderTest {

	public class Methods {
		public void voidMethod() {
		}

		public int callableMethod() {
			return 42;
		}

		public int throwingCallable() throws Exception {
			throw new Exception("test");
		}

		public int oneParameter(int a) {
			return 0;
		}

		public void oneParameter(Object a) {
		}

		public void oneParameter(Class<?> a) {
		}

		public boolean twoParameters(boolean a, boolean b) {
			return true;
		}

		public int twoParameters(int a, int b) {
			return 0;
		}

		public boolean threeParameters(boolean a, int b, boolean c) {
			return true;
		}

		public boolean threeParameters(boolean a, boolean b, boolean c) {
			return true;
		}

		public boolean fourParameters(boolean a, boolean b, boolean c, int d) {
			return true;
		}

		public boolean fourParameters(boolean a, boolean b, int c, int d) {
			return true;
		}

		public int allTypes(SomeClass sc, Object a, boolean b, Boolean c, byte d, Byte e, short f, Short g, char h,
				Character i, int j, Integer k, long l, Long m, float n, Float o, double p, Double q, Number r,
				Serializable s, Object[] t) {
			return 0;
		}

	}

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MethodRecorderTest.class);
	MethodRecorder<Methods> methodRecorder;

	@BeforeEach
	public void setup() {
		methodRecorder = new MethodRecorder<>(Methods.class);
	}

	@Test
	public void recordedClassReturned() {
		assert methodRecorder.getRecordedClass().equals(Methods.class);
	}

	@Test
	public void shouldRecordVoidMethodInvocation_Method() throws NoSuchMethodException, SecurityException {
		methodRecorder.getProxy().voidMethod();
		assert methodRecorder.getMethod().equals(Methods.class.getMethod("voidMethod"));
	}

	@Test
	public void shouldRecordVoidMethodInvocation_Method_RemainingMethodSignatures()
			throws NoSuchMethodException, SecurityException {
		methodRecorder.getProxy().voidMethod();
		Method expectedMethod = Methods.class.getMethod("voidMethod");
		assert methodRecorder.getMethod().equals(expectedMethod);
		assert methodRecorder.getMethod(new Object()).equals(expectedMethod);
		assert methodRecorder.getMethod(true).equals(expectedMethod);
		assert methodRecorder.getMethod((byte) 0).equals(expectedMethod);
		assert methodRecorder.getMethod((short) 0).equals(expectedMethod);
		assert methodRecorder.getMethod((char) 0).equals(expectedMethod);
		assert methodRecorder.getMethod(0).equals(expectedMethod); // int
		assert methodRecorder.getMethod((long) 0).equals(expectedMethod);
		assert methodRecorder.getMethod((float) 0).equals(expectedMethod);
		assert methodRecorder.getMethod((double) 0).equals(expectedMethod);
	}

	@Test
	public void shouldRecordVoidMethodInvocation_MethodName() throws NoSuchMethodException, SecurityException {
		methodRecorder.getProxy().voidMethod();
		assert methodRecorder.getMethodName().equals("voidMethod");
	}

	@Test
	public void shouldRecordVoidMethodInvocation_MethodName_RemainingMethodSignatures()
			throws NoSuchMethodException, SecurityException {
		methodRecorder.getProxy().voidMethod();
		String expectedMethodName = "voidMethod";
		assert methodRecorder.getMethodName().equals(expectedMethodName);
		assert methodRecorder.getMethodName(new Object()).equals(expectedMethodName);
		assert methodRecorder.getMethodName(true).equals(expectedMethodName);
		assert methodRecorder.getMethodName((byte) 0).equals(expectedMethodName);
		assert methodRecorder.getMethodName((short) 0).equals(expectedMethodName);
		assert methodRecorder.getMethodName((char) 0).equals(expectedMethodName);
		assert methodRecorder.getMethodName(0).equals(expectedMethodName); // int
		assert methodRecorder.getMethodName((long) 0).equals(expectedMethodName);
		assert methodRecorder.getMethodName((float) 0).equals(expectedMethodName);
		assert methodRecorder.getMethodName((double) 0).equals(expectedMethodName);
	}

	@Test
	public void shouldRecordVoidMethodInvocation_MethodCall() throws NoSuchMethodException, SecurityException {
		methodRecorder.getProxy().voidMethod();
		assert methodRecorder.getMethodCall().equals(new MethodCall<>(Methods.class.getMethod("voidMethod")));
	}

	@Test
	public void shouldRecordVoidMethodInvocation_MethodCall_RemainingMethodSignatures()
			throws NoSuchMethodException, SecurityException {
		methodRecorder.getProxy().voidMethod();
		MethodCall<Method> expectedMethodCall = new MethodCall<>(Methods.class.getMethod("voidMethod"));
		assert methodRecorder.getMethodCall().equals(expectedMethodCall);
		assert methodRecorder.getMethodCall(new Object()).equals(expectedMethodCall);
		assert methodRecorder.getMethodCall(true).equals(expectedMethodCall);
		assert methodRecorder.getMethodCall((byte) 0).equals(expectedMethodCall);
		assert methodRecorder.getMethodCall((short) 0).equals(expectedMethodCall);
		assert methodRecorder.getMethodCall((char) 0).equals(expectedMethodCall);
		assert methodRecorder.getMethodCall(0).equals(expectedMethodCall); // int
		assert methodRecorder.getMethodCall((long) 0).equals(expectedMethodCall);
		assert methodRecorder.getMethodCall((float) 0).equals(expectedMethodCall);
		assert methodRecorder.getMethodCall((double) 0).equals(expectedMethodCall);
	}

	@Test
	public void shouldRecordVoidMethodInvocationFluently_Method() throws NoSuchMethodException, SecurityException {
		assert methodRecorder.getMethod(() -> methodRecorder.getProxy().voidMethod())
				.equals(Methods.class.getMethod("voidMethod"));
	}

	@Test
	public void shouldRecordVoidMethodInvocationFluently_MethodName() throws NoSuchMethodException, SecurityException {
		MethodRecorder<Methods> methodRecorder = MethodRecorder.recordInvocationsOn(Methods.class);
		assert methodRecorder.getMethodName(() -> methodRecorder.getProxy().voidMethod()).equals("voidMethod");
	}

	@Test
	public void shouldRecordVoidMethodInvocationFluently_MethodCall() throws NoSuchMethodException, SecurityException {
		assert methodRecorder.getMethodCall(() -> methodRecorder.getProxy().voidMethod())
				.equals(new MethodCall<>(Methods.class.getMethod("voidMethod")));
	}

	@Test
	public void allStateShouldBeResetAfterCallToReset() throws NoSuchMethodException, SecurityException {
		methodRecorder = new MethodRecorder<>(Methods.class);
		int hashAfterConstruction = methodRecorder.hashCode();
		assert methodRecorder.getMethod(() -> methodRecorder.getProxy().voidMethod())
				.equals(Methods.class.getMethod("voidMethod"));
		int hashAfterMethodRecording = methodRecorder.hashCode();
		methodRecorder.reset();
		int hashAfterReset = methodRecorder.hashCode();
		assert hashAfterReset == hashAfterConstruction && hashAfterReset != hashAfterMethodRecording;
	}

	@Test
	public void shouldRecordMethodByCallingCallable() throws NoSuchMethodException, SecurityException {
		assert methodRecorder.getMethodName(() -> methodRecorder.getProxy().callableMethod()).equals("callableMethod");
	}

	@Test
	public void shouldInvokeProxyAndNotRealMethod() throws NoSuchMethodException, SecurityException {
		// FatalTestException should not be thrown, since the real implementation is not
		// called but the proxy implementation
		assert methodRecorder.getMethodName(() -> methodRecorder.getProxy().throwingCallable())
				.equals("throwingCallable");
	}

	@Test
	public void shouldRecordArgumentWithProxyInvocation() throws NoSuchMethodException, SecurityException {
		assert methodRecorder.getMethodCall(methodRecorder.getProxy().oneParameter(11))
				.equals(new MethodCall<>(Methods.class.getMethod("oneParameter", int.class), 11));
	}

	@Test
	public void shouldRecordArgumentWithProxyInvocationWithNullArg() throws NoSuchMethodException, SecurityException {
		Object object = null;
		assert methodRecorder.getMethodCall(() -> methodRecorder.getProxy().oneParameter(object))
				.equals(new MethodCall<>(Methods.class.getMethod("oneParameter", Object.class), object));
	}

	@Test
	public void shouldRecordMatcherArgumentWithProxyInvocation() throws NoSuchMethodException, SecurityException {
		int randomIntermediateIdentifier = 0;
		Matcher<Integer> matcher = Matchers.any(int.class); // ! Hamcrest matchers do not implement equals(Object obj)
		assert methodRecorder
				.getMethodCall(methodRecorder.getProxy()
						.oneParameter((randomIntermediateIdentifier = methodRecorder
								.storeAndCreateIdInstanceOfTypeArgument(matcher, int.class))))
				.equals(new MethodCall<>(Methods.class.getMethod("oneParameter", int.class), matcher));
		log.info("Random identifier used to retrieve matcher: {}", randomIntermediateIdentifier);
	}

	@Test
	public void shouldRecordPredicateArgumentWithProxyInvocation() throws NoSuchMethodException, SecurityException {
		int randomIntermediateIdentifier = 0;
		Predicate<Integer> intPredicate = integer -> integer == 5;
		assert methodRecorder
				.getMethodCall(methodRecorder.getProxy()
						.oneParameter((randomIntermediateIdentifier = methodRecorder
								.storeAndCreateIdInstanceOfTypeArgument(intPredicate, int.class))))
				.equals(new MethodCall<>(Methods.class.getMethod("oneParameter", int.class), intPredicate));
		log.info("Random identifier used to retrieve matcher: {}", randomIntermediateIdentifier);
	}

	@Test
	public void shouldThrowAmbiguouslyDefinedMatchersExceptionForFalseNextToBooleanMatcher()
			throws NoSuchMethodException, SecurityException {
		Matcher<Boolean> matcher = Matchers.any(boolean.class);
		Assertions
				.assertThrows(AmbiguouslyDefinedMatchersException.class,
						() -> methodRecorder.getMethodCall(methodRecorder.getProxy().twoParameters(
								false /* identifier value for boolean.class */,
								methodRecorder.storeAndCreateIdInstanceOfTypeArgument(matcher, boolean.class))));
	}

	@Test
	public void shouldThrowAmbiguouslyDefinedMatchersExceptionForFalseNextToBooleanPredicate()
			throws NoSuchMethodException, SecurityException {
		Predicate<Boolean> booleanPredicate = bool -> bool;
		Assertions.assertThrows(AmbiguouslyDefinedMatchersException.class,
				() -> methodRecorder.getMethodCall(methodRecorder.getProxy().twoParameters(false,
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(booleanPredicate, boolean.class))));
	}

	@Test
	public void shouldThrowAmbiguouslyDefinedMatchersExceptionForFalseNextToBooleanMatcherAndOtherValueTypeInBetween()
			throws NoSuchMethodException, SecurityException {
		Matcher<Boolean> matcher = Matchers.any(boolean.class);
		Assertions.assertThrows(AmbiguouslyDefinedMatchersException.class,
				() -> methodRecorder.getMethodCall(methodRecorder.getProxy().threeParameters(false, 0,
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(matcher, boolean.class))));
	}

	@Test
	public void shouldNotThrowAmbiguouslyDefinedMatchersExceptionForFalseNextToBooleanMatcherIfMatcherArgumentPositionSpecified()
			throws NoSuchMethodException, SecurityException {
		Matcher<Boolean> matcher = Matchers.any(boolean.class);
		assert methodRecorder
				.getMethodCall(methodRecorder.getProxy().twoParameters(false,
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(matcher, boolean.class, 1)))
				.equals(new MethodCall<>(Methods.class.getMethod("twoParameters", boolean.class, boolean.class), false,
						matcher));
	}

	@Test
	public void shouldNotThrowAmbiguouslyDefinedMatchersExceptionForFalseNextToBooleanPredicateIfPredicateArgumentPositionSpecified()
			throws NoSuchMethodException, SecurityException {
		Predicate<Boolean> booleanPredicate = bool -> bool;
		assert methodRecorder
				.getMethodCall(methodRecorder.getProxy().twoParameters(false,
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(booleanPredicate, boolean.class, 1)))
				.equals(new MethodCall<>(Methods.class.getMethod("twoParameters", boolean.class, boolean.class), false,
						booleanPredicate));
	}

	@Test
	public void shouldThrowAmbiguouslyDefinedMatchersExceptionWhenAmbiguousAndOneMatcherArguementPositionNotSpecified()
			throws NoSuchMethodException, SecurityException {
		Matcher<Boolean> matcher = Matchers.any(boolean.class);
		Assertions.assertThrows(AmbiguouslyDefinedMatchersException.class,
				() -> methodRecorder.getMethodCall(methodRecorder.getProxy().threeParameters(false,
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(matcher, boolean.class, 1),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(matcher, boolean.class))));
	}

	@Test
	public void shouldNotThrowAmbiguouslyDefinedMatchersExceptionWhenAllMatcherArgumentPositionsAreSpecified()
			throws NoSuchMethodException, SecurityException {
		Matcher<Boolean> matcher = Matchers.any(boolean.class);
		assert methodRecorder
				.getMethodCall(methodRecorder.getProxy().threeParameters(false,
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(matcher, boolean.class, 1),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(matcher, boolean.class, 2)))
				.equals(new MethodCall<>(
						Methods.class.getMethod("threeParameters", boolean.class, boolean.class, boolean.class), false,
						matcher, matcher));
	}

	@Test
	public void shouldNotThrowAmbiguouslyDefinedMatchersExceptionWhenAllArgumentsForTypesWithMatchersAreMatchers()
			throws NoSuchMethodException, SecurityException {
		Matcher<Boolean> matcher = Matchers.any(boolean.class);
		assert methodRecorder
				.getMethodCall(methodRecorder.getProxy().fourParameters(
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(matcher, boolean.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(matcher, boolean.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(matcher, boolean.class), 0))
				.equals(new MethodCall<>(Methods.class.getMethod("fourParameters", boolean.class, boolean.class,
						boolean.class, int.class), matcher, matcher, matcher, 0));
	}

	@Test
	public void shouldNotThrowAmbiguouslyDefinedMatchersExceptionForIntegerValueNextToIntegerMatcher()
			throws NoSuchMethodException, SecurityException {
		Matcher<Integer> matcher = Matchers.any(int.class);
		assert methodRecorder
				.getMethodCall(methodRecorder.getProxy().twoParameters(11,
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(matcher, int.class)))
				.equals(new MethodCall<>(Methods.class.getMethod("twoParameters", int.class, int.class), 11, matcher));
	}

	@Test
	public void shouldThrowAmbiguouslyDefinedMatchersExceptionOnlyForBooleanTypes_Test1of2()
			throws NoSuchMethodException, SecurityException {
		Matcher<Boolean> booleanMatcher = Matchers.any(boolean.class);
		Matcher<Integer> intMatcher = Matchers.any(int.class);
		Assertions.assertThrows(AmbiguouslyDefinedMatchersException.class,
				() -> methodRecorder.getMethodCall(
						methodRecorder.getProxy().fourParameters(false /* identifier value for boolean.class */,
								methodRecorder.storeAndCreateIdInstanceOfTypeArgument(booleanMatcher, boolean.class), 0,
								methodRecorder.storeAndCreateIdInstanceOfTypeArgument(intMatcher, int.class))));
	}

	@Test
	public void shouldThrowAmbiguouslyDefinedMatchersExceptionOnlyForBooleanTypes_Test2of2()
			throws NoSuchMethodException, SecurityException {
		Matcher<Boolean> booleanMatcher = Matchers.any(boolean.class);
		Matcher<Integer> intMatcher = Matchers.any(int.class);
		methodRecorder
				.getMethodCall(
						methodRecorder.getProxy().fourParameters(true /* NOT identifier value for boolean.class */,
								methodRecorder.storeAndCreateIdInstanceOfTypeArgument(booleanMatcher, boolean.class), 0,
								methodRecorder.storeAndCreateIdInstanceOfTypeArgument(intMatcher, int.class)))
				.equals(new MethodCall<>(
						Methods.class.getMethod("fourParameters", boolean.class, boolean.class, int.class, int.class),
						true, booleanMatcher, 0, intMatcher));
	}

	@Test
	public void forAllPossibleReferenceTypesRandomIdentifierValuesShouldBeGenerated()
			throws NoSuchMethodException, SecurityException {
		Matcher<SomeClass> someClassMatcher = Matchers.any(SomeClass.class); // class defined in same package
		Matcher<Object> objectMatcher = Matchers.any(Object.class); // object reference, not wrapper
		Matcher<Object[]> objectArrayMatcher = Matchers.any(Object[].class);
		Matcher<Serializable> serializableMatcher = Matchers.any(Serializable.class); // interface
		Matcher<Number> numberMatcher = Matchers.any(Number.class); // abstract class
		Matcher<Boolean> booleanMatcher = Matchers.any(Boolean.class);
		Matcher<Byte> byteMatcher = Matchers.any(Byte.class);
		Matcher<Short> shortMatcher = Matchers.any(Short.class);
		Matcher<Character> characterMatcher = Matchers.any(Character.class);
		Matcher<Integer> integerMatcher = Matchers.any(Integer.class);
		Matcher<Long> longMatcher = Matchers.any(Long.class);
		Matcher<Float> floatMatcher = Matchers.any(Float.class);
		Matcher<Double> doubleMatcher = Matchers.any(Double.class);
		Assertions.assertEquals(new MethodCall<>(
				Methods.class.getMethod("allTypes", SomeClass.class, Object.class, boolean.class, Boolean.class,
						byte.class, Byte.class, short.class, Short.class, char.class, Character.class, int.class,
						Integer.class, long.class, Long.class, float.class, Float.class, double.class, Double.class,
						Number.class, Serializable.class, Object[].class),
				someClassMatcher, objectMatcher, booleanMatcher, booleanMatcher, byteMatcher, byteMatcher, shortMatcher,
				shortMatcher, characterMatcher, characterMatcher, integerMatcher, integerMatcher, longMatcher,
				longMatcher, floatMatcher, floatMatcher, doubleMatcher, doubleMatcher, numberMatcher,
				serializableMatcher, objectArrayMatcher),
				methodRecorder.getMethodCall(methodRecorder.getProxy().allTypes(
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(someClassMatcher, SomeClass.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(objectMatcher, Object.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(booleanMatcher, boolean.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(booleanMatcher, Boolean.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(byteMatcher, byte.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(byteMatcher, Byte.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(shortMatcher, short.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(shortMatcher, Short.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(characterMatcher, char.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(characterMatcher, Character.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(integerMatcher, int.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(integerMatcher, Integer.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(longMatcher, long.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(longMatcher, Long.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(floatMatcher, float.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(floatMatcher, Float.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(doubleMatcher, double.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(doubleMatcher, Double.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(numberMatcher, Number.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(serializableMatcher, Serializable.class),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(objectArrayMatcher, Object[].class))));
	}

	@Test
	public void forNonWrapperFinalClassesIllegalArgumentExceptionShouldBeThrown()
			throws NoSuchMethodException, SecurityException {
		@SuppressWarnings("rawtypes")
		Matcher<Class> classMatcher = Matchers.any(Class.class);
		Assertions.assertThrows(IllegalArgumentException.class, () -> executeMethodRecorderCapture(classMatcher));
	}

	@SuppressWarnings("rawtypes")
	public void executeMethodRecorderCapture(Matcher<Class> classMatcher) throws Throwable {
		try {
			methodRecorder.getMethodCall(() -> methodRecorder.getProxy()
					.oneParameter(methodRecorder.storeAndCreateIdInstanceOfTypeArgument(classMatcher, Class.class)));
		} catch (FatalTestException e) {
			throw e.getCause();
		}
	}

	@Test
	public void forObjectReferenceTypesRandomIdentifierValuesShouldBeGenerated()
			throws NoSuchMethodException, SecurityException {
		Matcher<Object> objectMatcher = Matchers.any(Object.class); // object reference, not wrapper
		Assertions.assertEquals(new MethodCall<>(Methods.class.getMethod("oneParameter", Object.class), objectMatcher),
				methodRecorder.getMethodCall(() -> methodRecorder.getProxy().oneParameter(
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(objectMatcher, Object.class))));
	}

}
