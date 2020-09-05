package com.github.skjolber.jsonfilter.jackson;

import com.github.skjolber.jsonfilter.JsonFilter;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import static com.google.common.truth.Truth.*;
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
		JacksonSinglePathMaxStringLengthJsonFilter filter = (JacksonSinglePathMaxStringLengthJsonFilter) JacksonJsonLogFilterBuilder.createInstance()
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
		JacksonSinglePathMaxStringLengthJsonFilter filter = (JacksonSinglePathMaxStringLengthJsonFilter) JacksonJsonLogFilterBuilder.createInstance()
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
