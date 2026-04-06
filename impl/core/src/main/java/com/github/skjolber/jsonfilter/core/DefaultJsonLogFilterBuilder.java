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
package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractJsonLogFilterBuilder;

/**
 * Fluent builder for the default (high-performance, non-Jackson) {@linkplain JsonFilter}.
 *
 * <p>All filters produced by this builder are thread-safe and can be reused freely.
 *
 * <p>Quick one-liner factory methods for the most common cases:
 * <pre>{@code
 * // Anonymize fields by name at any depth
 * JsonFilter f = DefaultJsonLogFilterBuilder.anonymizeKeys("password", "ssn");
 *
 * // Anonymize fields by precise JSONPath
 * JsonFilter f = DefaultJsonLogFilterBuilder.anonymizePaths("$.customer.email");
 *
 * // Remove whole subtrees by name at any depth
 * JsonFilter f = DefaultJsonLogFilterBuilder.pruneKeys("rawPayload");
 *
 * // Remove whole subtrees by precise JSONPath
 * JsonFilter f = DefaultJsonLogFilterBuilder.prunePaths("$.context.auditLog");
 * }</pre>
 *
 * <p>Use {@link #newBuilder()} for more control:
 * <pre>{@code
 * JsonFilter filter = DefaultJsonLogFilterBuilder.newBuilder()
 *     .withAnonymizeKeys("password", "ssn")
 *     .withAnonymizePaths("$.customer.email")
 *     .withPrunePaths("$.context.rawPayload")
 *     .withAnonymizeMessage("[redacted]")
 *     .withMaxStringLength(256)
 *     .withMaxSize(128 * 1024)
 *     .build();
 * }</pre>
 */
public class DefaultJsonLogFilterBuilder extends AbstractJsonLogFilterBuilder<DefaultJsonLogFilterBuilder> {

	/**
	 * Create a new builder instance.
	 *
	 * @return a fresh {@linkplain DefaultJsonLogFilterBuilder}
	 */
	public static DefaultJsonLogFilterBuilder newBuilder() {
		return new DefaultJsonLogFilterBuilder();
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
	 * Create a filter that anonymizes the values at the given JSONPath expressions.
	 *
	 * @param expressions one or more JSONPath expressions (e.g. {@code "$.customer.email"})
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter anonymizePaths(String... expressions) {
		return newBuilder().withAnonymizePaths(expressions).build();
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
	 * Create a filter that removes (prunes) the subtrees at the given JSONPath expressions.
	 *
	 * @param expressions one or more JSONPath expressions (e.g. {@code "$.context.auditLog"})
	 * @return a ready-to-use, thread-safe filter
	 */
	public static JsonFilter prunePaths(String... expressions) {
		return newBuilder().withPrunePaths(expressions).build();
	}

	/**
	 * @deprecated Use {@link #newBuilder()} instead.
	 */
	@Deprecated
	public static DefaultJsonLogFilterBuilder createInstance() {
		return new DefaultJsonLogFilterBuilder();
	}

	@Override
	public JsonFilter build() {
		DefaultJsonFilterFactory factory = new DefaultJsonFilterFactory();

		factory.setMaxStringLength(maxStringLength);
		factory.setMaxPathMatches(maxPathMatches);

		factory.setAnonymize(anonymizeFilters);
		factory.setPrune(pruneFilters);

		factory.setPruneJsonValue(pruneJsonValue);
		factory.setAnonymizeJsonValue(anonymizeJsonValue);
		factory.setTruncateJsonStringValue(truncateStringValue);

		factory.setMaxSize(maxSize);
		factory.setRemoveWhitespace(removeWhitespace);

		return factory.newJsonFilter();
	}
}
