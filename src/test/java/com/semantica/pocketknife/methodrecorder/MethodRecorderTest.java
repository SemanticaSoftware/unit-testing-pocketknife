package com.semantica.pocketknife.methodrecorder;

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

		public boolean twoBooleanParameters(boolean a, boolean b) {
			return true;
		}

		public boolean threeBooleanParameters(boolean a, boolean b, boolean c) {
			return true;
		}

		public boolean fourParameters(boolean a, boolean b, boolean c, int d) {
			return true;
		}

		public int twoIntParameters(int a, int b) {
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
	public void shouldRecordVoidMethodInvocation_Method() throws NoSuchMethodException, SecurityException {
		methodRecorder.getProxy().voidMethod();
		assert methodRecorder.getMethod().equals(Methods.class.getMethod("voidMethod"));
	}

	@Test
	public void shouldRecordVoidMethodInvocation_MethodName() throws NoSuchMethodException, SecurityException {
		methodRecorder.getProxy().voidMethod();
		assert methodRecorder.getMethodName().equals("voidMethod");
	}

	@Test
	public void shouldRecordVoidMethodInvocation_MethodCall() throws NoSuchMethodException, SecurityException {
		methodRecorder.getProxy().voidMethod();
		assert methodRecorder.getMethodCall().equals(new MethodCall<>(Methods.class.getMethod("voidMethod")));
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
	public void shouldThrowAmbiguouslyDefinedMatchersExceptionForFalseNextToBooleanMatcher()
			throws NoSuchMethodException, SecurityException {
		Matcher<Boolean> matcher = Matchers.any(boolean.class);
		Assertions.assertThrows(AmbiguouslyDefinedMatchersException.class,
				() -> methodRecorder.getMethodCall(methodRecorder.getProxy().twoBooleanParameters(false,
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(matcher, boolean.class))));
	}

	@Test
	public void shouldNotThrowAmbiguouslyDefinedMatchersExceptionForFalseNextToBooleanMatcherIfMatcherArgumentPositionSpecified()
			throws NoSuchMethodException, SecurityException {
		Matcher<Boolean> matcher = Matchers.any(boolean.class);
		assert methodRecorder
				.getMethodCall(methodRecorder.getProxy().twoBooleanParameters(false,
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(matcher, boolean.class, 1)))
				.equals(new MethodCall<>(Methods.class.getMethod("twoBooleanParameters", boolean.class, boolean.class),
						false, matcher));
	}

	@Test
	public void shouldThrowAmbiguouslyDefinedMatchersExceptionWhenAmbiguousAndOneMatcherArguementPositionNotSpecified()
			throws NoSuchMethodException, SecurityException {
		Matcher<Boolean> matcher = Matchers.any(boolean.class);
		Assertions.assertThrows(AmbiguouslyDefinedMatchersException.class,
				() -> methodRecorder.getMethodCall(methodRecorder.getProxy().threeBooleanParameters(false,
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(matcher, boolean.class, 1),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(matcher, boolean.class))));
	}

	@Test
	public void shouldNotThrowAmbiguouslyDefinedMatchersExceptionWhenAllMatcherArgumentPositionsAreSpecified()
			throws NoSuchMethodException, SecurityException {
		Matcher<Boolean> matcher = Matchers.any(boolean.class);
		assert methodRecorder
				.getMethodCall(methodRecorder.getProxy().threeBooleanParameters(false,
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(matcher, boolean.class, 1),
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(matcher, boolean.class, 2)))
				.equals(new MethodCall<>(
						Methods.class.getMethod("threeBooleanParameters", boolean.class, boolean.class, boolean.class),
						false, matcher, matcher));
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
				.getMethodCall(methodRecorder.getProxy().twoIntParameters(11,
						methodRecorder.storeAndCreateIdInstanceOfTypeArgument(matcher, int.class)))
				.equals(new MethodCall<>(Methods.class.getMethod("twoIntParameters", int.class, int.class), 11,
						matcher));
	}

}
