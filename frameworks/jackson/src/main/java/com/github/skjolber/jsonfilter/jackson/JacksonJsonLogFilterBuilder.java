package com.github.skjolber.jsonfilter.jackson;

import java.util.Set;

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
 * // Anonymize fields by name at any depth — varargs
 * JsonFilter f = JacksonJsonLogFilterBuilder.anonymizeKeys("password", "ssn");
 *
 * // Anonymize fields by name — Set with optional size limits
 * JsonFilter f = JacksonJsonLogFilterBuilder.anonymizeKeys(Set.of("password", "ssn"));
 * JsonFilter f = JacksonJsonLogFilterBuilder.anonymizeKeys(Set.of("password", "ssn"), 256);
 * JsonFilter f = JacksonJsonLogFilterBuilder.anonymizeKeys(Set.of("password", "ssn"), 256, 128 * 1024);
 *
 * // Remove whole subtrees by field name — Set with optional size limits
 * JsonFilter f = JacksonJsonLogFilterBuilder.pruneKeys(Set.of("appMeta", "diagnostics"));
 * JsonFilter f = JacksonJsonLogFilterBuilder.pruneKeys(Set.of("appMeta"), 256, 128 * 1024);
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
	// One-liners: anonymize by key name
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
	 * Create a filter that anonymizes every field matching any key in the set,
	 * at any depth in the document.
	 *
	 * @param keys set of bare field names (e.g. {@code Set.of("password", "ssn")})
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter anonymizeKeys(Set<String> keys) {
		return newBuilder().withAnonymizeKeys(keys).build();
	}

	/**
	 * Create a filter that anonymizes fields by key name and truncates long strings.
	 *
	 * @param keys            set of bare field names
	 * @param maxStringLength maximum string value length before truncation
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter anonymizeKeys(Set<String> keys, int maxStringLength) {
		return newBuilder().withAnonymizeKeys(keys).withMaxStringLength(maxStringLength).build();
	}

	/**
	 * Create a filter that anonymizes fields by key name, truncates long strings,
	 * and limits the total output size.
	 *
	 * @param keys            set of bare field names
	 * @param maxStringLength maximum string value length before truncation
	 * @param maxSize         maximum output document size in bytes
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter anonymizeKeys(Set<String> keys, int maxStringLength, int maxSize) {
		return newBuilder().withAnonymizeKeys(keys).withMaxStringLength(maxStringLength).withMaxSize(maxSize).build();
	}

	// -------------------------------------------------------------------------
	// One-liners: anonymize by JSONPath
	// -------------------------------------------------------------------------

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
	 * Create a filter that anonymizes the values at the given JSONPath expressions.
	 *
	 * @param expressions set of JSONPath expressions
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter anonymizePaths(Set<String> expressions) {
		return newBuilder().withAnonymizePaths(expressions).build();
	}

	/**
	 * Create a filter that anonymizes fields by JSONPath and truncates long strings.
	 *
	 * @param expressions     set of JSONPath expressions
	 * @param maxStringLength maximum string value length before truncation
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter anonymizePaths(Set<String> expressions, int maxStringLength) {
		return newBuilder().withAnonymizePaths(expressions).withMaxStringLength(maxStringLength).build();
	}

	/**
	 * Create a filter that anonymizes fields by JSONPath, truncates long strings,
	 * and limits the total output size.
	 *
	 * @param expressions     set of JSONPath expressions
	 * @param maxStringLength maximum string value length before truncation
	 * @param maxSize         maximum output document size in bytes
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter anonymizePaths(Set<String> expressions, int maxStringLength, int maxSize) {
		return newBuilder().withAnonymizePaths(expressions).withMaxStringLength(maxStringLength).withMaxSize(maxSize).build();
	}

	// -------------------------------------------------------------------------
	// One-liners: prune by key name
	// -------------------------------------------------------------------------

	/**
	 * Create a filter that removes (prunes) every subtree whose field name matches
	 * any of the given keys, at any depth in the document.
	 *
	 * @param keys one or more bare field names (e.g. {@code "appMeta"})
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter pruneKeys(String... keys) {
		return newBuilder().withPruneKeys(keys).build();
	}

	/**
	 * Create a filter that removes (prunes) every subtree whose field name matches
	 * any key in the set, at any depth in the document.
	 *
	 * @param keys set of bare field names (e.g. {@code Set.of("appMeta", "diagnostics")})
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter pruneKeys(Set<String> keys) {
		return newBuilder().withPruneKeys(keys).build();
	}

	/**
	 * Create a filter that prunes fields by key name and truncates long strings.
	 *
	 * @param keys            set of bare field names
	 * @param maxStringLength maximum string value length before truncation
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter pruneKeys(Set<String> keys, int maxStringLength) {
		return newBuilder().withPruneKeys(keys).withMaxStringLength(maxStringLength).build();
	}

	/**
	 * Create a filter that prunes fields by key name, truncates long strings,
	 * and limits the total output size.
	 *
	 * @param keys            set of bare field names
	 * @param maxStringLength maximum string value length before truncation
	 * @param maxSize         maximum output document size in bytes
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter pruneKeys(Set<String> keys, int maxStringLength, int maxSize) {
		return newBuilder().withPruneKeys(keys).withMaxStringLength(maxStringLength).withMaxSize(maxSize).build();
	}

	// -------------------------------------------------------------------------
	// One-liners: prune by JSONPath
	// -------------------------------------------------------------------------

	/**
	 * Create a filter that removes (prunes) the subtrees at the given JSONPath expressions.
	 *
	 * @param expressions one or more JSONPath expressions (e.g. {@code "$.context.appMeta"})
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter prunePaths(String... expressions) {
		return newBuilder().withPrunePaths(expressions).build();
	}

	/**
	 * Create a filter that removes (prunes) the subtrees at the given JSONPath expressions.
	 *
	 * @param expressions set of JSONPath expressions
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter prunePaths(Set<String> expressions) {
		return newBuilder().withPrunePaths(expressions).build();
	}

	/**
	 * Create a filter that prunes by JSONPath and truncates long strings.
	 *
	 * @param expressions     set of JSONPath expressions
	 * @param maxStringLength maximum string value length before truncation
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter prunePaths(Set<String> expressions, int maxStringLength) {
		return newBuilder().withPrunePaths(expressions).withMaxStringLength(maxStringLength).build();
	}

	/**
	 * Create a filter that prunes by JSONPath, truncates long strings,
	 * and limits the total output size.
	 *
	 * @param expressions     set of JSONPath expressions
	 * @param maxStringLength maximum string value length before truncation
	 * @param maxSize         maximum output document size in bytes
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter prunePaths(Set<String> expressions, int maxStringLength, int maxSize) {
		return newBuilder().withPrunePaths(expressions).withMaxStringLength(maxStringLength).withMaxSize(maxSize).build();
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
