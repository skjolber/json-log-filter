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
 * <p>Quick one-liner factory methods for the most common cases:
 * <pre>{@code
 * // Anonymize fields by name at any depth
 * JsonFilter f = JacksonJsonLogFilterBuilder.anonymizeKeys("password", "ssn");
 *
 * // Anonymize fields by precise JSONPath
 * JsonFilter f = JacksonJsonLogFilterBuilder.anonymizePaths("$.customer.email");
 *
 * // Remove whole subtrees by name at any depth
 * JsonFilter f = JacksonJsonLogFilterBuilder.pruneKeys("rawPayload");
 *
 * // Remove whole subtrees by precise JSONPath
 * JsonFilter f = JacksonJsonLogFilterBuilder.prunePaths("$.context.auditLog");
 * }</pre>
 *
 * <p>Use {@link #newBuilder()} for more control:
 * <pre>{@code
 * JsonFilter filter = JacksonJsonLogFilterBuilder.newBuilder()
 *     .withAnonymizeKeys("password", "ssn")
 *     .withAnonymizePaths("$.customer.email")
 *     .withPrunePaths("$.context.rawPayload")
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

	// -------------------------------------------------------------------------
	// Static one-liner factory methods
	// -------------------------------------------------------------------------

	/**
	 * Create a filter that anonymizes every field matching any of the given keys,
	 * at any depth in the document.
	 *
	 * @param keys one or more bare field names (e.g. {@code "password"})
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter anonymizeKeys(String... keys) {
		return newBuilder().withAnonymizeKeys(keys).build();
	}

	/**
	 * Create a filter that anonymizes fields by key name and truncates long strings.
	 *
	 * @param maxStringLength maximum string value length before truncation
	 * @param keys            one or more bare field names
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter anonymizeKeys(int maxStringLength, String... keys) {
		return newBuilder().withMaxStringLength(maxStringLength).withAnonymizeKeys(keys).build();
	}

	/**
	 * Create a filter that anonymizes fields by key name, truncates long strings,
	 * and limits the total output size.
	 *
	 * @param maxStringLength maximum string value length before truncation
	 * @param maxSize         maximum output document size in bytes
	 * @param keys            one or more bare field names
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter anonymizeKeys(int maxStringLength, int maxSize, String... keys) {
		return newBuilder().withMaxStringLength(maxStringLength).withMaxSize(maxSize).withAnonymizeKeys(keys).build();
	}

	/**
	 * Create a filter that anonymizes the values at the given JSONPath expressions.
	 *
	 * @param expressions one or more JSONPath expressions (e.g. {@code "$.customer.email"})
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter anonymizePaths(String... expressions) {
		return newBuilder().withAnonymizePaths(expressions).build();
	}

	/**
	 * Create a filter that anonymizes fields by JSONPath and truncates long strings.
	 *
	 * @param maxStringLength maximum string value length before truncation
	 * @param expressions     one or more JSONPath expressions
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter anonymizePaths(int maxStringLength, String... expressions) {
		return newBuilder().withMaxStringLength(maxStringLength).withAnonymizePaths(expressions).build();
	}

	/**
	 * Create a filter that anonymizes fields by JSONPath, truncates long strings,
	 * and limits the total output size.
	 *
	 * @param maxStringLength maximum string value length before truncation
	 * @param maxSize         maximum output document size in bytes
	 * @param expressions     one or more JSONPath expressions
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter anonymizePaths(int maxStringLength, int maxSize, String... expressions) {
		return newBuilder().withMaxStringLength(maxStringLength).withMaxSize(maxSize).withAnonymizePaths(expressions).build();
	}

	/**
	 * Create a filter that removes (prunes) every subtree whose field name matches
	 * any of the given keys, at any depth in the document.
	 *
	 * @param keys one or more bare field names (e.g. {@code "rawPayload"})
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter pruneKeys(String... keys) {
		return newBuilder().withPruneKeys(keys).build();
	}

	/**
	 * Create a filter that prunes fields by key name and truncates long strings.
	 *
	 * @param maxStringLength maximum string value length before truncation
	 * @param keys            one or more bare field names
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter pruneKeys(int maxStringLength, String... keys) {
		return newBuilder().withMaxStringLength(maxStringLength).withPruneKeys(keys).build();
	}

	/**
	 * Create a filter that prunes fields by key name, truncates long strings,
	 * and limits the total output size.
	 *
	 * @param maxStringLength maximum string value length before truncation
	 * @param maxSize         maximum output document size in bytes
	 * @param keys            one or more bare field names
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter pruneKeys(int maxStringLength, int maxSize, String... keys) {
		return newBuilder().withMaxStringLength(maxStringLength).withMaxSize(maxSize).withPruneKeys(keys).build();
	}

	/**
	 * Create a filter that removes (prunes) the subtrees at the given JSONPath expressions.
	 *
	 * @param expressions one or more JSONPath expressions (e.g. {@code "$.context.auditLog"})
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter prunePaths(String... expressions) {
		return newBuilder().withPrunePaths(expressions).build();
	}

	/**
	 * Create a filter that prunes by JSONPath and truncates long strings.
	 *
	 * @param maxStringLength maximum string value length before truncation
	 * @param expressions     one or more JSONPath expressions
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter prunePaths(int maxStringLength, String... expressions) {
		return newBuilder().withMaxStringLength(maxStringLength).withPrunePaths(expressions).build();
	}

	/**
	 * Create a filter that prunes by JSONPath, truncates long strings,
	 * and limits the total output size.
	 *
	 * @param maxStringLength maximum string value length before truncation
	 * @param maxSize         maximum output document size in bytes
	 * @param expressions     one or more JSONPath expressions
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter prunePaths(int maxStringLength, int maxSize, String... expressions) {
		return newBuilder().withMaxStringLength(maxStringLength).withMaxSize(maxSize).withPrunePaths(expressions).build();
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
