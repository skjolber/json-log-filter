package com.github.skjolber.jsonfilter.core;

import static com.github.skjolber.jsonfilter.core.PathRulesAssertions.assertFiltered;

import org.junit.jupiter.api.Test;

/**
 * If a value is both pruned and anonymized, prune takes precedence.
 */

public class PrunePrecedenceJsonFilterTest {

	private static final String INPUT = "{\"a\":{\"internalKey\":\"valuevaluevalue\"},\"b\":1}";
	private static final String PRUNED = "{\"a\":\"[removed]\",\"b\":1}";

	@Test
	public void sameKey() {
		assertFiltered(PRUNED, INPUT, (b) -> b.withPruneKeys("a").withAnonymizeKeys("a"));
	}

	@Test
	public void samePath() {
		assertFiltered(PRUNED, INPUT, (b) -> b.withPrunePaths("$.a").withAnonymizePaths("$.a"));
	}

	@Test
	public void prunePathAnonymizeKey() {
		assertFiltered(PRUNED, INPUT, (b) -> b.withPrunePaths("$.a").withAnonymizeKeys("a"));
	}

	@Test
	public void anonymizePathPruneKey() {
		assertFiltered(PRUNED, INPUT, (b) -> b.withAnonymizePaths("$.a").withPruneKeys("a"));
	}
}
