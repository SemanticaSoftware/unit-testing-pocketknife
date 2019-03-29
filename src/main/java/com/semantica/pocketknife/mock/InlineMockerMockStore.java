package com.semantica.pocketknife.mock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.semantica.pocketknife.calls.Invoked;

public class InlineMockerMockStore implements InlineMocker.CapturedMatchersStore, InlineMocker.MockVerificationStore {

	private Invoked timesInvoked;
	private List<MatcherCapture<?>> matcherCaptures;

	public InlineMockerMockStore() {
		super();
		matcherCaptures = new ArrayList<>();
	}

	@Override
	public void setNumberOfTimesIncomingMethodIsExpectedToBeInvoked(Invoked timesInvoked) {
		this.timesInvoked = timesInvoked;
	}

	@Override
	public Invoked getNumberOfTimesIncomingMethodIsExpectedToBeInvoked() {
		return timesInvoked;
	}

	@Override
	public void clearNumberOfTimesIncomingMethodIsExpectedToBeInvoked() {
		timesInvoked = null;
	}

	@Override
	public Iterator<MatcherCapture<?>> getMatcherCapturesIterator() {
		return matcherCaptures.iterator();
	}

	@Override
	public <T> void storeMatcherCapture(Object matcher, Class<T> clazz, Optional<Integer> argumentNumber,
			T wiringIdentity) {
		matcherCaptures.add(new MatcherCapture<>(matcher, clazz, argumentNumber, wiringIdentity));
	}

	@Override
	public void clearMatcherCaptures() {
		matcherCaptures.clear();
	}

}
