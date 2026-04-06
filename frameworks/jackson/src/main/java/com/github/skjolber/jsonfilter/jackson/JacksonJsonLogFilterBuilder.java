package com.github.skjolber.jsonfilter.jackson;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractJsonLogFilterBuilder;

/**
 * Fluent builder for a Jackson-backed {@linkplain JsonFilter}.
 *
 * <p>The Jackson filter validates document structure during filtering, making it
 * suitable for untrusted (remotely produced) JSON. For locally produced JSON,
 * prefer {@code DefaultJsonLogFilterBuilder} for higher throughput.
 *
 * <p>All filters produced by this builder are thread-safe and can be reused freely.
 *
 * <pre>{@code
 * JsonFilter filter = JacksonJsonLogFilterBuilder.newBuilder()
 *     .withMaxStringLength(127)
 *     .withAnonymize("$.customer.email", "$.customer.ssn")
 *     .withPrune("$.internal.debug")
 *     .withAnonymizeMessage("[redacted]")
 *     .build();
 * }</pre>
 *
 * <p>Note: {@code maxPathMatches} is not supported by the Jackson filter and is ignored.
 */
public class JacksonJsonLogFilterBuilder extends AbstractJsonLogFilterBuilder<JacksonJsonLogFilterBuilder> {

	/**
	 * Create a new builder instance.
	 *
	 * @return a fresh {@linkplain JacksonJsonLogFilterBuilder}
	 */
	public static JacksonJsonLogFilterBuilder newBuilder() {
		return new JacksonJsonLogFilterBuilder();
	}

	/**
	 * @deprecated Use {@link #newBuilder()} instead.
	 */
	@Deprecated
	public static JacksonJsonLogFilterBuilder createInstance() {
		return new JacksonJsonLogFilterBuilder();
	}

	@Override
	public JsonFilter build() {
		JacksonJsonFilterFactory factory = new JacksonJsonFilterFactory();

		factory.setMaxStringLength(maxStringLength);
		factory.setMaxPathMatches(maxPathMatches);

		if(!anonymizeFilters.isEmpty()) {
			factory.setAnonymize(anonymizeFilters);
		}
		if(!pruneFilters.isEmpty()) {
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
