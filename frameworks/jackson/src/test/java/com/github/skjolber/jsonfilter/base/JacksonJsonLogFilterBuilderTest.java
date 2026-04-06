package com.github.skjolber.jsonfilter.base;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonJsonLogFilterBuilder;
import com.github.skjolber.jsonfilter.jackson.JacksonPathMaxStringLengthJsonFilter;

public class JacksonJsonLogFilterBuilderTest {

	@Test
	public void testBuilder() {
		JsonFilter filter = JacksonJsonLogFilterBuilder.newBuilder()
				.withMaxStringLength(127)
				.withAnonymizePaths("/customer/email")
				.withPrunePaths("/customer/account")
				.build();
		assertNotNull(filter);
	}

	@Test
	public void testAnonymizeMessage() {
		JacksonPathMaxStringLengthJsonFilter filter = (JacksonPathMaxStringLengthJsonFilter) JacksonJsonLogFilterBuilder.newBuilder()
				.withMaxStringLength(127)
				.withAnonymizePaths("/customer/email")
				.withAnonymizeMessage("x\nxxxx")
				.build();
		assertNotNull(filter);
		assertThat(new String(filter.getAnonymizeJsonValue())).isEqualTo("\"x\\nxxxx\"");
		assertThat(new String(filter.getPruneJsonValue())).isEqualTo("\"PRUNED\"");
		assertThat(new String(filter.getTruncateStringValue())).isEqualTo("... + ");
	}

	@Test
	public void testPruneMessage() {
		JacksonPathMaxStringLengthJsonFilter filter = (JacksonPathMaxStringLengthJsonFilter) JacksonJsonLogFilterBuilder.newBuilder()
				.withMaxStringLength(127)
				.withPrunePaths("/customer/email")
				.withPruneMessage("x\nxxxx")
				.build();
		assertNotNull(filter);
		assertThat(new String(filter.getPruneJsonValue())).isEqualTo("\"x\\nxxxx\"");
		assertThat(new String(filter.getAnonymizeJsonValue())).isEqualTo("\"*\"");
		assertThat(new String(filter.getTruncateStringValue())).isEqualTo("... + ");
	}

}
