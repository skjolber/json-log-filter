package com.github.skjolber.jsonfilter.base;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonJsonLogFilterBuilder;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonPathMaxSizeMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonPathMaxStringLengthJsonFilter;
public class JacksonJsonLogFilterBuilderTest {

	@Test
	public void testBuilder() {
		JsonFilter filter = JacksonJsonLogFilterBuilder.createInstance()
				.withMaxStringLength(127)
				.withAnonymize("/customer/email") // inserts ***** for values
				.withPrune("/customer/account") // removes whole subtree
				.build();
		assertNotNull(filter);
	}

	@Test
	public void testAnonymizeMessage() {
		JacksonPathMaxStringLengthJsonFilter filter = (JacksonPathMaxStringLengthJsonFilter) JacksonJsonLogFilterBuilder.createInstance()
				.withMaxStringLength(127)
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
		JacksonPathMaxStringLengthJsonFilter filter = (JacksonPathMaxStringLengthJsonFilter) JacksonJsonLogFilterBuilder.createInstance()
				.withMaxStringLength(127)
				.withPrune("/customer/email") // inserts ***** for values
				.withPruneStringValue("x\nxxxx")
				.build();
		assertNotNull(filter);
		assertThat(new String(filter.getPruneJsonValue())).isEqualTo("\"x\\nxxxx\"");
		assertThat(new String(filter.getAnonymizeJsonValue())).isEqualTo("\"*\"");
		assertThat(new String(filter.getTruncateStringValue())).isEqualTo("... + ");
	}
	
}
