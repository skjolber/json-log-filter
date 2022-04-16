package com.github.skjolber.jsonfilter.jackson;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
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
	public void testTruncateMesseage() {
		JacksonMaxStringLengthJsonFilter filter = (JacksonMaxStringLengthJsonFilter) JacksonJsonLogFilterBuilder.createInstance()
				.withMaxStringLength(127)
				.withTruncateStringValue("truncated\t")
				.build();
		assertNotNull(filter);
		assertThat(new String(filter.getTruncateStringValue())).isEqualTo("truncated\\t");
		assertThat(new String(filter.getPruneJsonValue())).isEqualTo("\"SUBTREE REMOVED\"");
		assertThat(new String(filter.getAnonymizeJsonValue())).isEqualTo("\"*****\"");
	}
	
	@Test
	public void testAnonymizeMessage() {
		JacksonSingleFullPathMaxStringLengthJsonFilter filter = (JacksonSingleFullPathMaxStringLengthJsonFilter) JacksonJsonLogFilterBuilder.createInstance()
				.withMaxStringLength(127)
				.withAnonymize("/customer/email") // inserts ***** for values
				.withAnonymizeStringValue("x\nxxxx")
				.build();
		assertNotNull(filter);
		assertThat(new String(filter.getAnonymizeJsonValue())).isEqualTo("\"x\\nxxxx\"");
		assertThat(new String(filter.getPruneJsonValue())).isEqualTo("\"SUBTREE REMOVED\"");
		assertThat(new String(filter.getTruncateStringValue())).isEqualTo("...TRUNCATED BY ");
	}
	
	@Test
	public void testPruneMessage() {
		JacksonSingleFullPathMaxStringLengthJsonFilter filter = (JacksonSingleFullPathMaxStringLengthJsonFilter) JacksonJsonLogFilterBuilder.createInstance()
				.withMaxStringLength(127)
				.withPrune("/customer/email") // inserts ***** for values
				.withPruneStringValue("x\nxxxx")
				.build();
		assertNotNull(filter);
		assertThat(new String(filter.getPruneJsonValue())).isEqualTo("\"x\\nxxxx\"");
		assertThat(new String(filter.getAnonymizeJsonValue())).isEqualTo("\"*****\"");
		assertThat(new String(filter.getTruncateStringValue())).isEqualTo("...TRUNCATED BY ");
	}
	
}
