package com.semantica.pocketknife;

import java.lang.reflect.Method;

public class CallsFactory {

	private enum CallType {
		Strict, Default;
	}

	private enum MethodIdentifierType {
		String(String.class), Method(Method.class);

		private Class<?> keyClass;

		private MethodIdentifierType(Class<?> keyClass) {
			this.keyClass = keyClass;
		}

		public Class<?> getKeyClass() {
			return keyClass;
		}
	}

	public static Calls<?> getCalls(CallType callType, MethodIdentifierType methodIdentifierType) {
		switch (callType) {
		case Strict:
			switch (methodIdentifierType) {
			case String:
				return new StrictCalls<String>(methodIdentifierType.getKeyClass());
			case Method:
			}

			break;
		case Default:
			break;
		default:
			break;
		}
	}

}
