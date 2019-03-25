package com.semantica.pocketknife.methodrecorder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;

/**
 * Utility class for {@link MethodRecorder}. Contains a single method
 * {@link #checkForIdentifierAmbiguity(Object[], Map)} that checks an argument
 * array against a map of matchers to see whether there exists any ambiguity
 * when {@link Matcher}s or {@link Predicate}s (wrapped by
 * {@link MethodRecorder#storeAndCreateIdInstanceOfTypeArgument(Matcher, Class)}
 * or
 * {@link MethodRecorder#storeAndCreateIdInstanceOfTypeArgument(Predicate, Class)})
 * have been used as method arguments.
 *
 * @author A. Haanstra
 *
 */
class AmbiguousArgumentsUtil {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AmbiguousArgumentsUtil.class);

	static class AmbiguouslyDefinedMatchersException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public AmbiguouslyDefinedMatchersException(String message) {
			super(message);
		}

	}

	static void checkForIdentifierAmbiguity(Object[] args,
			Map<Class<?>, Map<Object, Queue<MatchingArgument>>> matchers) {
		Set<Class<?>> identifierTypesWithAmbiguousIdentifiers = new HashSet<>();
		for (Class<?> identifierClass : matchers.keySet()) {
			int numberOfMatchersForIdentifierClass = matchers.get(identifierClass).values().stream()
					.map(queue -> queue.size()).reduce((size1, size2) -> size1 + size2).orElse(0);
			if (numberOfIdentifierValuesInArgs(args, matchers, identifierClass) > numberOfMatchersForIdentifierClass) {
				Object ambiguousIdentifier = null;
				if ((ambiguousIdentifier = twoIdentifierValuesNextToEachOther(args, matchers,
						identifierClass)) != null) {
					log.error(
							"Identifier value \"{}\" is ambiguous for value type {} and Predicate<{}> and/or Matcher<{}>. Please specify argument numbers on *all* matching arguments for this type OR use matchers as arguments for all parameters of this type.",
							ambiguousIdentifier, ambiguousIdentifier.getClass(), ambiguousIdentifier.getClass(),
							ambiguousIdentifier.getClass());
					identifierTypesWithAmbiguousIdentifiers.add(identifierClass);
				}
			}
		}
		if (identifierTypesWithAmbiguousIdentifiers.size() > 0) {
			throw new AmbiguouslyDefinedMatchersException("Identifier values for "
					+ identifierTypesWithAmbiguousIdentifiers.size() + " value types ("
					+ identifierTypesWithAmbiguousIdentifiers
					+ ") are ambiguously defined. Please specify argument numbers on *all* matching arguments for this type OR use matchers as arguments for all parameters of this type. See previous log errors for more details.");
		}
	}

	private static int numberOfIdentifierValuesInArgs(Object[] args,
			Map<Class<?>, Map<Object, Queue<MatchingArgument>>> matchers, Class<?> identifierClass) {
		int numberOfIdentifierValuesInArgs = 0;
		for (Object identifier : matchers.get(identifierClass).keySet()) {
			for (Object arg : args) {
				if (identifier.equals(arg)) {
					numberOfIdentifierValuesInArgs++;
				}
			}
		}
		return numberOfIdentifierValuesInArgs;
	}

	private static Object twoIdentifierValuesNextToEachOther(Object[] args,
			Map<Class<?>, Map<Object, Queue<MatchingArgument>>> matchers, Class<?> identifierClass) {
		List<Optional<Object>> argsOnlyIdentifiersElseEmpty = constructArgsListWithOnlyIdentifiersForAllIdentifierTypes(
				args, matchers);
		return twoIdentifierValuesNextToEachOther(argsOnlyIdentifiersElseEmpty, matchers, identifierClass);
	}

	private static List<Optional<Object>> constructArgsListWithOnlyIdentifiersForAllIdentifierTypes(Object[] args,
			Map<Class<?>, Map<Object, Queue<MatchingArgument>>> matchers) {
		Set<Object> identifierValues = matchers.values().stream().flatMap(map -> map.keySet().stream())
				.collect(Collectors.toSet());
		List<Optional<Object>> argsOnlyIdentifiersElseEmpty = new ArrayList<>(args.length);
		for (Object arg : args) {
			if (identifierValues.contains(arg)) {
				argsOnlyIdentifiersElseEmpty.add(Optional.of(arg));
			} else {
				argsOnlyIdentifiersElseEmpty.add(Optional.empty());
			}
		}
		return argsOnlyIdentifiersElseEmpty;
	}

	private static Object twoIdentifierValuesNextToEachOther(List<Optional<Object>> argsOnlyIdentifiersElseEmpty,
			Map<Class<?>, Map<Object, Queue<MatchingArgument>>> matchers, Class<?> identifierClass) {
		Object previousIdentifier = null;
		for (Optional<Object> identifierElseEmpty : argsOnlyIdentifiersElseEmpty) {
			try {
				Object currentIdentifier = identifierElseEmpty.get();
				if (identifierClass.isInstance(currentIdentifier) && currentIdentifier.equals(previousIdentifier)) {
					Object ambiguousIdentifier = currentIdentifier;
					long ambiguousMatchersWithoutArgumentNumberSpecification = matchers
							.get(ambiguousIdentifier.getClass()).get(ambiguousIdentifier).stream()
							.filter(matchingArgument -> !matchingArgument.getArgumentNumber().isPresent()).count();
					if (ambiguousMatchersWithoutArgumentNumberSpecification > 0L) {
						return currentIdentifier;
					}
				}
				previousIdentifier = currentIdentifier;
			} catch (NoSuchElementException e) {
				continue;
			}
		}
		return null;
	}

}
