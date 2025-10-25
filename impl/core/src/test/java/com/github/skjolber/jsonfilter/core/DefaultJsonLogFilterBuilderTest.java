package com.github.skjolber.jsonfilter.core;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;

public class DefaultJsonLogFilterBuilderTest {

	@Test
	public void testBuilder() {
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)DefaultJsonLogFilterBuilder.createInstance()
				.withMaxStringLength(127)
				.withAnonymize("/customer/email") // inserts ***** for values
				.withPrune("/customer/account") // removes whole subtree
				.withPruneStringValue("pruneMessage")
				.withAnonymizeStringValue("pruneMessage")
				.withTruncateStringValue("truncateMessage")
				.withMaxPathMatches(10)
				.withMaxSize(32 * 1024)
				.build();
		assertNotNull(filter);
		
		assertThat(filter.getMaxStringLength()).isEqualTo(127);
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/customer/email"});
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/customer/account"});
		assertThat(filter.getMaxPathMatches()).isEqualTo(10);
		assertThat(filter.getMaxSize()).isEqualTo(32 * 1024);
	}
	
	@Test
	public void testTruncateMessage() {
		MaxStringLengthJsonFilter filter = (MaxStringLengthJsonFilter) DefaultJsonLogFilterBuilder.createInstance()
				.withMaxStringLength(127)
				.withTruncateStringValue("truncated\t")
				.build();
		assertNotNull(filter);
		assertThat(new String(filter.getTruncateStringValue())).isEqualTo("truncated\\t");
		assertThat(new String(filter.getPruneJsonValue())).isEqualTo("\"PRUNED\"");
		assertThat(new String(filter.getAnonymizeJsonValue())).isEqualTo("\"*\"");
	}
	
	
}
