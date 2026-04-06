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
				.withAnonymizePaths("/customer/email")
				.withPrunePaths("/customer/account")
				.build();
		assertNotNull(filter);
	}

	@Test
	public void testAnonymizeKeys() {
		JsonFilter filter = JacksonJsonLogFilterBuilder.newBuilder()
				.withAnonymizeKeys("password", "ssn")
				.build();
		assertNotNull(filter);
	}

	@Test
	public void testPruneKeys() {
		JsonFilter filter = JacksonJsonLogFilterBuilder.newBuilder()
				.withPruneKeys("rawPayload", "auditLog")
				.build();
		assertNotNull(filter);
	}

	@Test
	public void testAnonymizeKeysCollection() {
		List<String> keys = Arrays.asList("password", "ssn");
		JsonFilter filter = JacksonJsonLogFilterBuilder.newBuilder()
				.withAnonymizeKeys(keys)
				.build();
		assertNotNull(filter);
	}

	@Test
	public void testPruneKeysCollection() {
		List<String> keys = Arrays.asList("rawPayload", "auditLog");
		JsonFilter filter = JacksonJsonLogFilterBuilder.newBuilder()
				.withPruneKeys(keys)
				.build();
		assertNotNull(filter);
	}

	@Test
	public void testAnonymizePathsVarargs() {
		JsonFilter filter = JacksonJsonLogFilterBuilder.newBuilder()
				.withAnonymizePaths("$.customer.email", "$.customer.ssn")
				.build();
		assertNotNull(filter);
	}

	@Test
	public void testPrunePathsVarargs() {
		JsonFilter filter = JacksonJsonLogFilterBuilder.newBuilder()
				.withPrunePaths("$.internal.debug", "$.internal.trace")
				.build();
		assertNotNull(filter);
	}

	@Test
	public void testAnonymizePathsCollection() {
		List<String> paths = Arrays.asList("$.customer.email", "$.customer.ssn");
		JsonFilter filter = JacksonJsonLogFilterBuilder.newBuilder()
				.withAnonymizePaths(paths)
				.build();
		assertNotNull(filter);
	}

	@Test
	public void testPrunePathsCollection() {
		List<String> paths = Arrays.asList("$.internal.debug", "$.internal.trace");
		JsonFilter filter = JacksonJsonLogFilterBuilder.newBuilder()
				.withPrunePaths(paths)
				.build();
		assertNotNull(filter);
	}

	@Test
	public void testStaticAnonymizeKeys() {
		assertNotNull(JacksonJsonLogFilterBuilder.anonymizeKeys("password", "ssn"));
	}

	@Test
	public void testStaticAnonymizePaths() {
		assertNotNull(JacksonJsonLogFilterBuilder.anonymizePaths("$.customer.email"));
	}

	@Test
	public void testStaticPruneKeys() {
		assertNotNull(JacksonJsonLogFilterBuilder.pruneKeys("rawPayload"));
	}

	@Test
	public void testStaticPrunePaths() {
		assertNotNull(JacksonJsonLogFilterBuilder.prunePaths("$.context.auditLog"));
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
