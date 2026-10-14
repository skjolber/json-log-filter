package com.github.skjolber.jsonfilter.core.ws;

import static com.google.common.truth.Truth.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.DefaultJsonLogFilterBuilder;

/**
 * Whitespace must also be removed from subtrees which are deeper than any path expression.
 */

public class RemoveWhitespaceDeepSubtreeJsonFilterTest {

	private static final String INPUT = "{\"a\":{\"b\":{\"c\":\"x\"\n,\"d\":\"y\"\n}}}";
	private static final String EXPECTED = "{\"a\":{\"b\":{\"c\":\"x\",\"d\":\"y\"}}}";

	@Test
	public void shortValuesFollowedByWhitespace() {
		assertFiltered(EXPECTED, INPUT, DefaultJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.z").withRemoveWhitespace(true).build());
	}

	@Test
	public void shortValuesFollowedByWhitespaceWithMaxStringLength() {
		assertFiltered(EXPECTED, INPUT, DefaultJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.z").withMaxStringLength(10).withRemoveWhitespace(true).build());
	}

	@Test
	public void mixedValuesFollowedByWhitespace() {
		String input = "{\"a\":{\"b\":{\"c\" : \"x\" ,\n\t\"d\": \"abcdefghijklmnopqrstuvwxyz\"\r\n, \"e\":\"y\"  }  }\n}";
		String expected = "{\"a\":{\"b\":{\"c\":\"x\",\"d\":\"abcdefghij... + 16\",\"e\":\"y\"}}}";

		assertFiltered(expected, input, DefaultJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.z").withMaxStringLength(10).withRemoveWhitespace(true).build());
	}

	private static void assertFiltered(String expected, String input, JsonFilter filter) {
		assertThat(filter.process(input)).isEqualTo(expected);
		assertThat(new String(filter.process(input.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)).isEqualTo(expected);
	}
}
