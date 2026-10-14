package com.github.skjolber.jsonfilter.jackson;

import static com.github.skjolber.jsonfilter.jackson.JacksonPathRulesAssertions.assertFiltered;

import org.junit.jupiter.api.Test;

/**
 * Slash and dot notation can be mixed.
 */

public class JacksonPathNotationJsonFilterTest {

	private static final String USER = "{\"user\":{\"password\":\"hunter2hunter2\",\"address\":{\"street\":\"Main Street 1\",\"zip\":\"12345678\"}}}";

	@Test
	public void shorterPathCoversLongerPath() {
		assertFiltered("{\"user\":{\"password\":\"hunter2hunter2\",\"address\":\"[removed]\"}}", USER,
				(b) -> b.withPrunePaths("/user/address").withAnonymizePaths("$.user.address.zip"));
	}

	@Test
	public void samePathPrunePrecedence() {
		assertFiltered("{\"user\":{\"password\":\"hunter2hunter2\",\"address\":\"[removed]\"}}", USER,
				(b) -> b.withPrunePaths("/user/address").withAnonymizePaths("$.user.address"));
	}
}
