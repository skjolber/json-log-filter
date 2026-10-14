package com.github.skjolber.jsonfilter.jackson;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Max path matches must be positive, or -1 for no limit, also for Jackson, which otherwise ignores the setting.
 */

public class JacksonMaxPathMatchesValidationTest {

	@Test
	public void zeroIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> new JacksonJsonFilterFactory().setMaxPathMatches(0));
		assertThrows(IllegalArgumentException.class, () -> JacksonJsonLogFilterBuilder.createInstance().withAnonymizeKeys("a").withMaxPathMatches(0).build());
	}
}
