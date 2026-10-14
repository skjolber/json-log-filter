package com.github.skjolber.jsonfilter.jackson;

import static com.github.skjolber.jsonfilter.jackson.JacksonPathRulesAssertions.assertFiltered;

import org.junit.jupiter.api.Test;

/**
 * A wildcard which is filtered as a whole also covers its explicit siblings.
 */

public class JacksonWildcardPathJsonFilterTest {

	@Test
	public void wildcardCoversExplicitSibling() {
		String input = "{\"user\":{\"password\":\"hunter2hunter2\",\"address\":{\"street\":\"Main Street 1\",\"zip\":\"12345678\"}}}";
		assertFiltered("{\"user\":{\"password\":\"*\",\"address\":{\"street\":\"*\",\"zip\":\"*\"}}}", input,
				(b) -> b.withAnonymizePaths("$.user.*", "$.user.address.zip"));
	}

	@Test
	public void wildcardCoversExplicitSiblingBelowWildcard() {
		String input = "{\"user\":{\"password\":{\"x\":\"secretsecret\",\"y\":\"secretsecret\"},\"name\":\"Name\"}}";
		assertFiltered("{\"user\":{\"password\":\"[removed]\",\"name\":\"Name\"}}", input,
				(b) -> b.withPrunePaths("$.*.password").withAnonymizePaths("$.user.password.x"));
	}
}
