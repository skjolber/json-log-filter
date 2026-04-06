/***************************************************************************
 * Copyright 2020 Thomas Rorvik Skjolberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.github.skjolber.jsonfilter.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base builder for {@linkplain com.github.skjolber.jsonfilter.JsonFilter} instances.
 *
 * <p>Provides a fluent API for configuring path-based filtering (anonymize/prune),
 * output customization, and size constraints. Concrete subclasses supply the
 * {@link #build()} method that wires the settings into a specific filter implementation.
 *
 * <p>Example usage:
 * <pre>{@code
 * JsonFilter filter = DefaultJsonLogFilterBuilder.newBuilder()
 *     .withMaxStringLength(127)
 *     .withAnonymize("$.customer.email", "$.customer.ssn")
 *     .withPrune("$.customer.account", "$.internal.debug")
 *     .withAnonymizeMessage("[redacted]")
 *     .withPruneMessage("[removed]")
 *     .withMaxSize(128 * 1024)
 *     .build();
 * }</pre>
 *
 * @param <B> the concrete builder type, for fluent chaining
 */
public abstract class AbstractJsonLogFilterBuilder<B extends AbstractJsonLogFilterBuilder<B>> {

	protected int maxStringLength = -1;
	protected int maxPathMatches = -1;
	protected int maxSize = -1;
	protected boolean removeWhitespace = false;

	protected List<String> anonymizeFilters = new ArrayList<>();
	protected List<String> pruneFilters = new ArrayList<>();

	/** Raw JSON value used to replace pruned nodes */
	protected String pruneJsonValue;
	/** Raw JSON value used to replace anonymized values */
	protected String anonymizeJsonValue;
	/** Raw (escaped) JSON string suffix appended after truncated text */
	protected String truncateStringValue;

	@SuppressWarnings("unchecked")
	protected B self() {
		return (B) this;
	}

	/**
	 * Build and return a configured, thread-safe filter instance.
	 *
	 * @return a new {@linkplain com.github.skjolber.jsonfilter.JsonFilter}
	 */
	public abstract com.github.skjolber.jsonfilter.JsonFilter build();

	// -------------------------------------------------------------------------
	// Size / limit settings
	// -------------------------------------------------------------------------

	/**
	 * Limit the length of string values in the output. Strings exceeding this
	 * length are truncated and a suffix (configurable via {@link #withTruncateMessage})
	 * is appended.
	 *
	 * @param length maximum number of characters per string value
	 * @return this builder
	 */
	public B withMaxStringLength(int length) {
		this.maxStringLength = length;
		return self();
	}

	/**
	 * Stop path-based filtering after this many matches. Useful when the target
	 * field appears a known number of times near the beginning of the document,
	 * as the filter can then skip the remainder at near pass-through speed.
	 *
	 * @param maxPathMatches maximum number of path matches before filtering stops
	 * @return this builder
	 */
	public B withMaxPathMatches(int maxPathMatches) {
		this.maxPathMatches = maxPathMatches;
		return self();
	}

	/**
	 * Limit the size of the output document. Content beyond the limit is dropped.
	 *
	 * @param maxSize maximum output size in bytes
	 * @return this builder
	 */
	public B withMaxSize(int maxSize) {
		this.maxSize = maxSize;
		return self();
	}

	/**
	 * Remove all insignificant whitespace from the output.
	 *
	 * @param removeWhitespace {@code true} to strip whitespace
	 * @return this builder
	 */
	public B withRemoveWhitespace(boolean removeWhitespace) {
		this.removeWhitespace = removeWhitespace;
		return self();
	}

	// -------------------------------------------------------------------------
	// Path filters – anonymize
	// -------------------------------------------------------------------------

	/**
	 * Anonymize the values at the given JSONPath expressions. Scalar values are
	 * replaced by the anonymize placeholder (default {@code "*"}); object and
	 * array values have all their nested scalars replaced recursively.
	 *
	 * <p>May be called multiple times to accumulate expressions:
	 * <pre>{@code
	 * builder.withAnonymize("$.customer.email")
	 *        .withAnonymize("$.customer.phone");
	 * }</pre>
	 * or in a single call:
	 * <pre>{@code
	 * builder.withAnonymize("$.customer.email", "$.customer.phone");
	 * }</pre>
	 *
	 * @param filters one or more JSONPath expressions
	 * @return this builder
	 */
	public B withAnonymize(String... filters) {
		Collections.addAll(anonymizeFilters, filters);
		return self();
	}

	/**
	 * Anonymize the values at the given JSONPath expressions.
	 *
	 * @param filters collection of JSONPath expressions
	 * @return this builder
	 * @see #withAnonymize(String...)
	 */
	public B withAnonymize(Collection<String> filters) {
		anonymizeFilters.addAll(filters);
		return self();
	}

	// -------------------------------------------------------------------------
	// Path filters – prune
	// -------------------------------------------------------------------------

	/**
	 * Remove (prune) the subtrees at the given JSONPath expressions. The entire
	 * value – whether a scalar, object, or array – is replaced by the prune
	 * placeholder (default {@code "PRUNED"}).
	 *
	 * <p>May be called multiple times to accumulate expressions:
	 * <pre>{@code
	 * builder.withPrune("$.internal.debug")
	 *        .withPrune("$.internal.trace");
	 * }</pre>
	 * or in a single call:
	 * <pre>{@code
	 * builder.withPrune("$.internal.debug", "$.internal.trace");
	 * }</pre>
	 *
	 * @param filters one or more JSONPath expressions
	 * @return this builder
	 */
	public B withPrune(String... filters) {
		Collections.addAll(pruneFilters, filters);
		return self();
	}

	/**
	 * Remove (prune) the subtrees at the given JSONPath expressions.
	 *
	 * @param filters collection of JSONPath expressions
	 * @return this builder
	 * @see #withPrune(String...)
	 */
	public B withPrune(Collection<String> filters) {
		pruneFilters.addAll(filters);
		return self();
	}

	// -------------------------------------------------------------------------
	// Output customization
	// -------------------------------------------------------------------------

	/**
	 * Set the replacement text inserted in place of pruned subtrees.
	 * The value is automatically JSON-escaped and wrapped in quotes.
	 *
	 * <p>Default: {@code "PRUNED"}
	 *
	 * @param value the unescaped replacement text (e.g. {@code "[removed]"})
	 * @return this builder
	 */
	public B withPruneMessage(String value) {
		StringBuilder sb = new StringBuilder(value.length() * 2);
		sb.append('"');
		AbstractJsonFilter.quoteAsString(value, sb);
		sb.append('"');
		return withPruneRawJsonValue(sb.toString());
	}

	/**
	 * Set the replacement text inserted in place of anonymized values.
	 * The value is automatically JSON-escaped and wrapped in quotes.
	 *
	 * <p>Default: {@code "*"}
	 *
	 * @param value the unescaped replacement text (e.g. {@code "[redacted]"})
	 * @return this builder
	 */
	public B withAnonymizeMessage(String value) {
		StringBuilder sb = new StringBuilder(value.length() * 2);
		sb.append('"');
		AbstractJsonFilter.quoteAsString(value, sb);
		sb.append('"');
		return withAnonymizeRawJsonValue(sb.toString());
	}

	/**
	 * Set the suffix appended after truncated string values.
	 * The value is automatically JSON-escaped (but not quoted, as it is
	 * embedded inside an existing string value).
	 *
	 * <p>Default: {@code "... + "} followed by the number of omitted characters.
	 *
	 * @param value the unescaped suffix text (e.g. {@code "…"})
	 * @return this builder
	 */
	public B withTruncateMessage(String value) {
		StringBuilder sb = new StringBuilder(value.length() * 2);
		AbstractJsonFilter.quoteAsString(value, sb);
		return withTruncateRawJsonStringValue(sb.toString());
	}

	// -------------------------------------------------------------------------
	// Raw JSON value overrides (for advanced / pre-escaped use)
	// -------------------------------------------------------------------------

	/**
	 * Set the raw JSON value used to replace pruned nodes.
	 * The value must be valid, pre-escaped JSON (e.g. {@code "\"PRUNED\""}).
	 *
	 * @param raw a valid JSON value
	 * @return this builder
	 */
	public B withPruneRawJsonValue(String raw) {
		this.pruneJsonValue = raw;
		return self();
	}

	/**
	 * Set the raw JSON value used to replace anonymized values.
	 * The value must be valid, pre-escaped JSON (e.g. {@code "\"*\""}).
	 *
	 * @param raw a valid JSON value
	 * @return this builder
	 */
	public B withAnonymizeRawJsonValue(String raw) {
		this.anonymizeJsonValue = raw;
		return self();
	}

	/**
	 * Set the raw (pre-escaped) JSON string suffix appended after truncated text.
	 * The value must already be JSON-string-escaped (no surrounding quotes).
	 *
	 * @param escaped a pre-escaped JSON string fragment
	 * @return this builder
	 */
	public B withTruncateRawJsonStringValue(String escaped) {
		this.truncateStringValue = escaped;
		return self();
	}

	// -------------------------------------------------------------------------
	// Deprecated aliases for backwards compatibility
	// -------------------------------------------------------------------------

	/**
	 * @deprecated Use {@link #withPruneMessage(String)} instead.
	 */
	@Deprecated
	public B withPruneStringValue(String value) {
		return withPruneMessage(value);
	}

	/**
	 * @deprecated Use {@link #withAnonymizeMessage(String)} instead.
	 */
	@Deprecated
	public B withAnonymizeStringValue(String value) {
		return withAnonymizeMessage(value);
	}

	/**
	 * @deprecated Use {@link #withTruncateMessage(String)} instead.
	 */
	@Deprecated
	public B withTruncateStringValue(String value) {
		return withTruncateMessage(value);
	}
}
