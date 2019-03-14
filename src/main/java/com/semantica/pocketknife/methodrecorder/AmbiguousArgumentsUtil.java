package com.semantica.pocketknife.methodrecorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

class AmbiguousArgumentsUtil {

	static class AmbiguouslyDefinedMatchersException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public AmbiguouslyDefinedMatchersException(String message) {
			super(message);
		}

	}

	static void checkForIdentifierAmbiguity(Object[] args, Set<Object> identifierValues,
			Map<Class<?>, Map<Object, Queue<MatchingArgument>>> matchers) {
		int numberOfMatchers = matchers.values().stream().flatMap(map -> map.values().stream())
				.map(queue -> queue.size()).reduce((size1, size2) -> size1 + size2).orElse(0);
		if (numberOfIdentifierValuesInArgs(args, identifierValues) > numberOfMatchers) {
			Object ambiguousIdentifier = null;
			if ((ambiguousIdentifier = twoIdentifierValuesNextToEachOther(args, identifierValues)) != null) {
				long ambiguousMatchersWithoutArgumentNumberSpecification = matchers.get(ambiguousIdentifier.getClass())
						.get(ambiguousIdentifier).stream()
						.filter(matchingArgument -> !matchingArgument.getArgumentNumber().isPresent()).count();
				if (ambiguousMatchersWithoutArgumentNumberSpecification != 0L) {
					throw new AmbiguouslyDefinedMatchersException("Identifier value \"" + ambiguousIdentifier
							+ "\" is ambiguous for value type " + ambiguousIdentifier.getClass() + " and Predicate<"
							+ ambiguousIdentifier.getClass() + "> and/or Matcher<" + ambiguousIdentifier.getClass()
							+ ">. Please specify argument numbers on *all* matching arguments for this type.");
				}
			}
		}
	}

	private static int numberOfIdentifierValuesInArgs(Object[] args, Set<Object> identifierValues) {
		int numberOfIdentifierValuesInArgs = 0;
		for (Object identifier : identifierValues) {
			for (Object arg : args) {
				if (identifier.equals(arg)) {
					numberOfIdentifierValuesInArgs++;
				}
			}
		}
		return numberOfIdentifierValuesInArgs;
	}

	private static Object twoIdentifierValuesNextToEachOther(Object[] args, Set<Object> identifierValues) {
		List<Optional<Object>> argsOnlyIdentifiersElseEmpty = constructArgsListWithOnlyIdentifiers(args,
				identifierValues);
		return twoIdentifierValuesNextToEachOther(argsOnlyIdentifiersElseEmpty);
	}

	private static List<Optional<Object>> constructArgsListWithOnlyIdentifiers(Object[] args,
			Set<Object> identifierValues) {
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

	private static Object twoIdentifierValuesNextToEachOther(List<Optional<Object>> argsOnlyIdentifiersElseEmpty) {
		Object previousIdentifier = null;
		for (Optional<Object> identifierElseEmpty : argsOnlyIdentifiersElseEmpty) {
			try {
				Object currentIdentifier = identifierElseEmpty.get();
				if (currentIdentifier.equals(previousIdentifier)) {
					return currentIdentifier;
				}
				previousIdentifier = currentIdentifier;
			} catch (NoSuchElementException e) {
				continue;
			}
		}
		return null;
	}

}
