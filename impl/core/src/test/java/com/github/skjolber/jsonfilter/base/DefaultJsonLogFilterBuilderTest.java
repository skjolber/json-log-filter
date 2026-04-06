package com.github.skjolber.jsonfilter.base;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.core.AbstractRangesPathJsonFilter;
import com.github.skjolber.jsonfilter.core.DefaultJsonLogFilterBuilder;

public class DefaultJsonLogFilterBuilderTest {

	@Test
	public void testAnonymizeMessage() {
		AbstractRangesPathJsonFilter filter = (AbstractRangesPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
				.withAnonymizePaths("/customer/email")
				.withAnonymizeMessage("x\nxxxx")
				.build();
		assertNotNull(filter);
		assertThat(new String(filter.getAnonymizeJsonValue())).isEqualTo("\"x\\nxxxx\"");
		assertThat(new String(filter.getPruneJsonValue())).isEqualTo("\"[removed]\"");
		assertThat(new String(filter.getTruncateStringValue())).isEqualTo("... + ");
	}

	@Test
	public void testPruneMessage() {
		AbstractRangesPathJsonFilter filter = (AbstractRangesPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
				.withPrunePaths("/customer/email")
				.withPruneMessage("x\nxxxx")
				.build();
		assertNotNull(filter);
		assertThat(new String(filter.getPruneJsonValue())).isEqualTo("\"x\\nxxxx\"");
		assertThat(new String(filter.getAnonymizeJsonValue())).isEqualTo("\"*\"");
		assertThat(new String(filter.getTruncateStringValue())).isEqualTo("... + ");
	}

}
