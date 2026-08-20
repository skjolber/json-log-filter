package com.github.skjolber.jsonfilter.simdjson;

import java.util.Set;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractJsonLogFilterBuilder;

/**
 * Fluent builder for a simdjson-java backed {@linkplain JsonFilter}.
 *
 * <p>The simdjson filter uses the high-performance simdjson-java DOM parser for
 * parsing and validation, making it suitable for untrusted JSON. For locally
 * produced JSON, prefer {@code DefaultJsonLogFilterBuilder} for higher throughput.
 *
 * <p>All filters produced by this builder are thread-safe and can be reused freely.
 *
 * <p>Quick one-liner factory methods for the most common cases:
 * <pre>{@code
 * JsonFilter f = SimdJsonJsonLogFilterBuilder.anonymizeKeys("password", "ssn");
 * JsonFilter f = SimdJsonJsonLogFilterBuilder.pruneKeys(Set.of("appMeta", "diagnostics"));
 * }</pre>
 *
 * <p>Use {@link #newBuilder()} for more control:
 * <pre>{@code
 * JsonFilter filter = SimdJsonJsonLogFilterBuilder.newBuilder()
 *     .withAnonymizeKeys("password", "ssn")
 *     .withPrunePaths("$.context.rawPayload")
 *     .build();
 * }</pre>
 */
public class SimdJsonJsonLogFilterBuilder extends AbstractJsonLogFilterBuilder<SimdJsonJsonLogFilterBuilder> {

	public static SimdJsonJsonLogFilterBuilder newBuilder() {
		return new SimdJsonJsonLogFilterBuilder();
	}

	public static JsonFilter anonymizeKeys(String... keys) {
		return newBuilder().withAnonymizeKeys(keys).build();
	}

	public static JsonFilter anonymizeKeys(Set<String> keys) {
		return newBuilder().withAnonymizeKeys(keys).build();
	}

	public static JsonFilter anonymizeKeys(Set<String> keys, int maxStringLength) {
		return newBuilder().withAnonymizeKeys(keys).withMaxStringLength(maxStringLength).build();
	}

	public static JsonFilter pruneKeys(String... keys) {
		return newBuilder().withPruneKeys(keys).build();
	}

	public static JsonFilter pruneKeys(Set<String> keys) {
		return newBuilder().withPruneKeys(keys).build();
	}

	public static JsonFilter pruneKeys(Set<String> keys, int maxStringLength) {
		return newBuilder().withPruneKeys(keys).withMaxStringLength(maxStringLength).build();
	}

	@Override
	public JsonFilter build() {
		SimdJsonJsonFilterFactory factory = new SimdJsonJsonFilterFactory();

		factory.setMaxStringLength(maxStringLength);
		factory.setMaxPathMatches(maxPathMatches);

		if (!anonymizeFilters.isEmpty()) {
			factory.setAnonymize(anonymizeFilters);
		}
		if (!pruneFilters.isEmpty()) {
			factory.setPrune(pruneFilters);
		}

		factory.setAnonymizeJsonValue(anonymizeJsonValue);
		factory.setPruneJsonValue(pruneJsonValue);
		factory.setTruncateJsonStringValue(truncateStringValue);

		factory.setMaxSize(maxSize);
		factory.setRemoveWhitespace(removeWhitespace);

		return factory.newJsonFilter();
	}
}
