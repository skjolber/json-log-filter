package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class DefaultJsonLogFilterBuilderTest {

	@Test
	public void testBuilder() {
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)DefaultJsonLogFilterBuilder.createInstance()
                .withMaxStringLength(127)
                .withAnonymize("/customer/email") // inserts ***** for values
                .withPrune("/customer/account") // removes whole subtree
                .withMaxPathMatches(10)
                .build();
		assertNotNull(filter);
		
		assertThat(filter.getMaxStringLength()).isEqualTo(127);
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/customer/email"});
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/customer/account"});
		assertThat(filter.getMaxPathMatches()).isEqualTo(10);
	}
}
