package com.github.skjolber.jsonfilter.jackson;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;

public class JacksonJsonLogFilterBuilderTest {

	@Test
	public void testBuilder() {
		JsonFilter filter = JacksonJsonLogFilterBuilder.newBuilder()
				.withMaxStringLength(127)
				.withAnonymize("/customer/email")
				.withPrune("/customer/account")
				.build();
		assertNotNull(filter);
	}

	@Test
	public void testMultiplePathsVarargs() {
		JsonFilter filter = JacksonJsonLogFilterBuilder.newBuilder()
				.withAnonymize("$.customer.email", "$.customer.ssn")
				.withPrune("$.internal.debug", "$.internal.trace")
				.build();
		assertNotNull(filter);
	}

	@Test
	public void testMultiplePathsCollection() {
		List<String> anonymize = Arrays.asList("$.customer.email", "$.customer.ssn");
		List<String> prune = Arrays.asList("$.internal.debug", "$.internal.trace");

		JsonFilter filter = JacksonJsonLogFilterBuilder.newBuilder()
				.withAnonymize(anonymize)
				.withPrune(prune)
				.build();
		assertNotNull(filter);
	}

	@Test
	public void testTruncateMessage() {
		JacksonMaxStringLengthJsonFilter filter = (JacksonMaxStringLengthJsonFilter) JacksonJsonLogFilterBuilder.newBuilder()
				.withMaxStringLength(127)
				.withTruncateMessage("truncated\t")
				.build();
		assertNotNull(filter);
		assertThat(new String(filter.getTruncateStringValue())).isEqualTo("truncated\\t");
		assertThat(new String(filter.getPruneJsonValue())).isEqualTo("\"PRUNED\"");
		assertThat(new String(filter.getAnonymizeJsonValue())).isEqualTo("\"*\"");
	}

	/** Verify deprecated aliases still work for backwards compatibility. */
	@Test
	@SuppressWarnings("deprecation")
	public void testDeprecatedAliases() {
		JacksonMaxStringLengthJsonFilter filter = (JacksonMaxStringLengthJsonFilter) JacksonJsonLogFilterBuilder.createInstance()
				.withMaxStringLength(127)
				.withTruncateStringValue("truncated\t")
				.build();
		assertNotNull(filter);
		assertThat(new String(filter.getTruncateStringValue())).isEqualTo("truncated\\t");
	}

}
