package com.semantica.pocketknife.mock.service.support;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.semantica.pocketknife.calls.Invoked;
import com.semantica.pocketknife.mock.service.InlineMockerCallVerificationStore;
import com.semantica.pocketknife.mock.service.InlineMockerCallVerificationStore.InvocationStore;

public class CallVerificationInvocationsStore
		implements InlineMockerCallVerificationStore.InvocationStore {

	private BlockingQueue<Invoked> timesInvoked = new ArrayBlockingQueue<>(1);

	@Override
	public void addNumberOfTimesIncomingMethodIsExpectedToBeInvoked(Invoked timesInvoked) {
		this.timesInvoked.add(timesInvoked);
	}

	@Override
	public Invoked removeNumberOfTimesIncomingMethodIsExpectedToBeInvoked() {
		return this.timesInvoked.remove();
	}
}
