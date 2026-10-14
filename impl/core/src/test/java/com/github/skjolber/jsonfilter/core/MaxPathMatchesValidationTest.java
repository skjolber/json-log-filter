package com.github.skjolber.jsonfilter.core;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;

/**
 * Max path matches must be positive, or -1 for no limit.
 */

public class MaxPathMatchesValidationTest {

	@Test
	public void validValues() {
		AbstractPathJsonFilter.validateMaxPathMatches(-1);
		AbstractPathJsonFilter.validateMaxPathMatches(1);
		AbstractPathJsonFilter.validateMaxPathMatches(5);
		DefaultJsonLogFilterBuilder.createInstance().withAnonymizeKeys("a").withMaxPathMatches(1).build();
	}

	@Test
	public void zeroIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> AbstractPathJsonFilter.validateMaxPathMatches(0));
		assertThrows(IllegalArgumentException.class, () -> new DefaultJsonFilterFactory().setMaxPathMatches(0));
		assertThrows(IllegalArgumentException.class, () -> DefaultJsonLogFilterBuilder.createInstance().withAnonymizeKeys("a").withMaxPathMatches(0).build());
		assertThrows(IllegalArgumentException.class, () -> DefaultJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.a").withMaxPathMatches(0).build());
		assertThrows(IllegalArgumentException.class, () -> new PathJsonFilter(0, new String[] {"$.a"}, null));
		assertThrows(IllegalArgumentException.class, () -> new AnyPathJsonFilter(0, new String[] {"$..a"}, null));
	}

	@Test
	public void belowMinusOneIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> AbstractPathJsonFilter.validateMaxPathMatches(-2));
		assertThrows(IllegalArgumentException.class, () -> new DefaultJsonFilterFactory().setMaxPathMatches(-2));
	}
}
