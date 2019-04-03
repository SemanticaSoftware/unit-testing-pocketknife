package com.semantica.pocketknife.mock;

import java.lang.reflect.Method;

import org.apache.commons.lang3.NotImplementedException;

import com.semantica.pocketknife.calls.Calls;
import com.semantica.pocketknife.calls.CallsFactory;
import com.semantica.pocketknife.calls.DefaultCalls;
import com.semantica.pocketknife.calls.StrictCalls;
import com.semantica.pocketknife.mock.service.InlineMockerCallVerificationStore;
import com.semantica.pocketknife.mock.service.InlineMockerCallVerificationStore.InvocationStore;
import com.semantica.pocketknife.mock.service.InlineMockerDelegatesStore;
import com.semantica.pocketknife.mock.service.InlineMockerInterceptionsStore;
import com.semantica.pocketknife.mock.service.InlineMockerMethodConverter;
import com.semantica.pocketknife.mock.service.InlineMockerMethodConverter.CapturedMatchersStore;
import com.semantica.pocketknife.mock.service.support.CallVerificationInvocationsStore;
import com.semantica.pocketknife.mock.service.support.MethodConverterMatchersStore;
import com.semantica.pocketknife.mock.service.support.components.DynamicMockingCallsRegistry;
import com.semantica.pocketknife.mock.service.support.components.DynamicMockingtrictCallsRegistry;

/**
 * Factory class for InlineMocker
 *
 */
public class InlineMockers {
	private static final DefaultCalls<Method> DUMMY_DEFAULT_CALLS = new DynamicMockingCallsRegistry<>(Method.class);
	private static final StrictCalls<Method> DUMMY_STRICT_CALLS = new DynamicMockingtrictCallsRegistry<>(Method.class);

	public static InlineMocker get(CallsFactory.CallType callType) {
		return new InlineMocker(interceptionsStore(), delegatesStore(), callRegistriesStore(callType),
				exactToMatchingMethodConverter());
	}

	private static InterceptionsStore interceptionsStore() {
		return new InlineMockerInterceptionsStore();
	}

	private static DelegatesStore delegatesStore() {
		return new InlineMockerDelegatesStore();
	}

	@SuppressWarnings("unchecked")
	private static VerificationStore<? extends Calls<Method>> callRegistriesStore(CallsFactory.CallType callType) {
		switch (callType) {
		case DEFAULT:
			return new InlineMockerCallVerificationStore<DefaultCalls<Method>>(
					(Class<DefaultCalls<Method>>) DUMMY_DEFAULT_CALLS.getClass(), invocationVerificationStore());
		case STRICT:
			return new InlineMockerCallVerificationStore<StrictCalls<Method>>(
					(Class<StrictCalls<Method>>) DUMMY_STRICT_CALLS.getClass(), invocationVerificationStore());
		default:
			throw new NotImplementedException(String.format("Unknown CallType: %s.", callType));
		}
	}

	private static InvocationStore invocationVerificationStore() {
		return new CallVerificationInvocationsStore();
	}

	private static ExactToMatchingMethodConverter exactToMatchingMethodConverter() {
		return new InlineMockerMethodConverter(capturedMatchersStore());
	}

	private static CapturedMatchersStore capturedMatchersStore() {
		return new MethodConverterMatchersStore();
	}

}