package com.github.skjolber.jsonfilter.core;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;

public class DefaultJsonLogFilterBuilderTest {

	@Test
	public void testBuilder() {
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
				.withMaxStringLength(127)
				.withAnonymize("/customer/email")
				.withPrune("/customer/account")
				.withPruneMessage("pruneMessage")
				.withAnonymizeMessage("pruneMessage")
				.withTruncateMessage("truncateMessage")
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
	public void testMultiplePathsVarargs() {
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
				.withAnonymize("$.customer.email", "$.customer.ssn")
				.withPrune("$.internal.debug", "$.internal.trace")
				.build();
		assertNotNull(filter);

		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"$.customer.email", "$.customer.ssn"});
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"$.internal.debug", "$.internal.trace"});
	}

	@Test
	public void testMultiplePathsCollection() {
		List<String> anonymize = Arrays.asList("$.customer.email", "$.customer.ssn");
		List<String> prune = Arrays.asList("$.internal.debug", "$.internal.trace");

		AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
				.withAnonymize(anonymize)
				.withPrune(prune)
				.build();
		assertNotNull(filter);

		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"$.customer.email", "$.customer.ssn"});
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"$.internal.debug", "$.internal.trace"});
	}

	@Test
	public void testTruncateMessage() {
		MaxStringLengthJsonFilter filter = (MaxStringLengthJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
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
		MaxStringLengthJsonFilter filter = (MaxStringLengthJsonFilter) DefaultJsonLogFilterBuilder.createInstance()
				.withMaxStringLength(127)
				.withPruneStringValue("PRUNED")
				.withAnonymizeStringValue("*")
				.withTruncateStringValue("truncated\t")
				.build();
		assertNotNull(filter);
		assertThat(new String(filter.getTruncateStringValue())).isEqualTo("truncated\\t");
	}

}
