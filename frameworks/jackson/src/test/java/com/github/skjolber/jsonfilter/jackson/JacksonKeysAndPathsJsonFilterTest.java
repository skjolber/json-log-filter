package com.github.skjolber.jsonfilter.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;

/**
 * Key rules (any-path, i.e. $..key) and full path rules can be combined.
 */

public class JacksonKeysAndPathsJsonFilterTest {

	private static final String INPUT = "{\"password\":\"secretsecret\",\"customer\":{\"email\":\"mail@example.com\",\"ssn\":\"123456789\"}}";

	private static final List<UnaryOperator<JacksonJsonLogFilterBuilder>> VARIANTS = List.of(
		(b) -> b,
		(b) -> b.withMaxStringLength(100),
		(b) -> b.withMaxSize(INPUT.length() - 1),
		(b) -> b.withMaxStringLength(100).withMaxSize(INPUT.length() - 1)
	);

	private static void assertFiltered(String expected, UnaryOperator<JacksonJsonLogFilterBuilder> configuration) {
		for(UnaryOperator<JacksonJsonLogFilterBuilder> variant : VARIANTS) {
			JsonFilter filter = variant.apply(configuration.apply(JacksonJsonLogFilterBuilder.createInstance())).build();
			String name = filter.toString();

			assertEquals(expected, filter.process(INPUT), name);
			assertEquals(expected, new String(filter.process(INPUT.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8), name);
		}
	}

	@Test
	public void anonymize() {
		assertFiltered("{\"password\":\"*\",\"customer\":{\"email\":\"*\",\"ssn\":\"123456789\"}}",
				(b) -> b.withAnonymizeKeys("password").withAnonymizePaths("$.customer.email"));
	}

	@Test
	public void prune() {
		assertFiltered("{\"password\":\"[removed]\",\"customer\":{\"email\":\"mail@example.com\",\"ssn\":\"[removed]\"}}",
				(b) -> b.withPruneKeys("password").withPrunePaths("$.customer.ssn"));
	}
}
