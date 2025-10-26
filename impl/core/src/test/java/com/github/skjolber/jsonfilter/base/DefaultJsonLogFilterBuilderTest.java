package com.github.skjolber.jsonfilter.base;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;
import com.github.skjolber.jsonfilter.core.AbstractRangesPathJsonFilter;
import com.github.skjolber.jsonfilter.core.DefaultJsonLogFilterBuilder;

public class DefaultJsonLogFilterBuilderTest {

	@Test
	public void testAnonymizeMessage() {
		AbstractRangesPathJsonFilter filter = (AbstractRangesPathJsonFilter) DefaultJsonLogFilterBuilder.createInstance()
				.withAnonymize("/customer/email") // inserts ***** for values
				.withAnonymizeStringValue("x\nxxxx")
				.build();
		assertNotNull(filter);
		assertThat(new String(filter.getAnonymizeJsonValue())).isEqualTo("\"x\\nxxxx\"");
		assertThat(new String(filter.getPruneJsonValue())).isEqualTo("\"PRUNED\"");
		assertThat(new String(filter.getTruncateStringValue())).isEqualTo("... + ");
	}
	
	@Test
	public void testPruneMessage() {
		AbstractRangesPathJsonFilter filter = (AbstractRangesPathJsonFilter) DefaultJsonLogFilterBuilder.createInstance()
				.withPrune("/customer/email") // inserts ***** for values
				.withPruneStringValue("x\nxxxx")
				.build();
		assertNotNull(filter);
		assertThat(new String(filter.getPruneJsonValue())).isEqualTo("\"x\\nxxxx\"");
		assertThat(new String(filter.getAnonymizeJsonValue())).isEqualTo("\"*\"");
		assertThat(new String(filter.getTruncateStringValue())).isEqualTo("... + ");
	}
	
}
