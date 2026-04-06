package com.github.skjolber.jsonfilter.core;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;

public class DefaultJsonLogFilterBuilderTest {

	@Test
	public void testBuilder() {
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
				.withMaxStringLength(127)
				.withAnonymizePaths("/customer/email")
				.withPrunePaths("/customer/account")
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
	public void testAnonymizeKeys() {
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
				.withAnonymizeKeys("password", "ssn")
				.build();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"$..password", "$..ssn"});
	}

	@Test
	public void testPruneKeys() {
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
				.withPruneKeys("rawPayload", "auditLog")
				.build();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"$..rawPayload", "$..auditLog"});
	}

	@Test
	public void testAnonymizeKeysCollection() {
		List<String> keys = Arrays.asList("password", "ssn");
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
				.withAnonymizeKeys(keys)
				.build();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"$..password", "$..ssn"});
	}

	@Test
	public void testPruneKeysCollection() {
		List<String> keys = Arrays.asList("rawPayload", "auditLog");
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
				.withPruneKeys(keys)
				.build();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"$..rawPayload", "$..auditLog"});
	}

	@Test
	public void testAnonymizePathsVarargs() {
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
				.withAnonymizePaths("$.customer.email", "$.customer.ssn")
				.build();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"$.customer.email", "$.customer.ssn"});
	}

	@Test
	public void testPrunePathsVarargs() {
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
				.withPrunePaths("$.internal.debug", "$.internal.trace")
				.build();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"$.internal.debug", "$.internal.trace"});
	}

	@Test
	public void testAnonymizePathsCollection() {
		List<String> paths = Arrays.asList("$.customer.email", "$.customer.ssn");
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
				.withAnonymizePaths(paths)
				.build();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"$.customer.email", "$.customer.ssn"});
	}

	@Test
	public void testPrunePathsCollection() {
		List<String> paths = Arrays.asList("$.internal.debug", "$.internal.trace");
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
				.withPrunePaths(paths)
				.build();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"$.internal.debug", "$.internal.trace"});
	}

	@Test
	public void testStaticAnonymizeKeys() {
		JsonFilter filter = DefaultJsonLogFilterBuilder.anonymizeKeys("password", "ssn");
		assertNotNull(filter);
	}

	@Test
	public void testStaticAnonymizeKeysWithMaxStringLength() {
		JsonFilter filter = DefaultJsonLogFilterBuilder.anonymizeKeys(256, "password", "ssn");
		assertNotNull(filter);
	}

	@Test
	public void testStaticAnonymizeKeysWithMaxStringLengthAndMaxSize() {
		JsonFilter filter = DefaultJsonLogFilterBuilder.anonymizeKeys(256, 128 * 1024, "password");
		assertNotNull(filter);
	}

	@Test
	public void testStaticAnonymizePaths() {
		JsonFilter filter = DefaultJsonLogFilterBuilder.anonymizePaths("$.customer.email");
		assertNotNull(filter);
	}

	@Test
	public void testStaticAnonymizePathsWithMaxStringLength() {
		JsonFilter filter = DefaultJsonLogFilterBuilder.anonymizePaths(256, "$.customer.email");
		assertNotNull(filter);
	}

	@Test
	public void testStaticAnonymizePathsWithMaxStringLengthAndMaxSize() {
		JsonFilter filter = DefaultJsonLogFilterBuilder.anonymizePaths(256, 128 * 1024, "$.customer.email");
		assertNotNull(filter);
	}

	@Test
	public void testStaticPruneKeys() {
		JsonFilter filter = DefaultJsonLogFilterBuilder.pruneKeys("rawPayload");
		assertNotNull(filter);
	}

	@Test
	public void testStaticPruneKeysWithMaxStringLength() {
		JsonFilter filter = DefaultJsonLogFilterBuilder.pruneKeys(256, "rawPayload");
		assertNotNull(filter);
	}

	@Test
	public void testStaticPruneKeysWithMaxStringLengthAndMaxSize() {
		JsonFilter filter = DefaultJsonLogFilterBuilder.pruneKeys(256, 128 * 1024, "rawPayload");
		assertNotNull(filter);
	}

	@Test
	public void testStaticPrunePaths() {
		JsonFilter filter = DefaultJsonLogFilterBuilder.prunePaths("$.context.auditLog");
		assertNotNull(filter);
	}

	@Test
	public void testStaticPrunePathsWithMaxStringLength() {
		JsonFilter filter = DefaultJsonLogFilterBuilder.prunePaths(256, "$.context.auditLog");
		assertNotNull(filter);
	}

	@Test
	public void testStaticPrunePathsWithMaxStringLengthAndMaxSize() {
		JsonFilter filter = DefaultJsonLogFilterBuilder.prunePaths(256, 128 * 1024, "$.context.auditLog");
		assertNotNull(filter);
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
