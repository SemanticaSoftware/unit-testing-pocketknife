package com.semantica.pocketknife.mock.service.support;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.semantica.pocketknife.mock.dto.MatcherCapture;
import com.semantica.pocketknife.mock.service.InlineMockerMethodConverter;
import com.semantica.pocketknife.mock.service.InlineMockerMethodConverter.CapturedMatchersStore;

public class MethodConverterMatchersStore implements InlineMockerMethodConverter.CapturedMatchersStore {

	private List<MatcherCapture<?>> matcherCaptures;

	public MethodConverterMatchersStore() {
		super();
		matcherCaptures = new ArrayList<>();
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
