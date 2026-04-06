/***************************************************************************
 * Copyright 2020 Thomas Rorvik Skjolberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.github.skjolber.jsonfilter.core;

import java.util.Set;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractJsonLogFilterBuilder;

/**
 * Fluent builder for the default (high-performance, non-Jackson) {@linkplain JsonFilter}.
 *
 * <p>All filters produced by this builder are thread-safe and can be reused freely.
 *
 * <p>Quick one-liner factory methods for the most common cases:
 * <pre>{@code
 * // Anonymize fields by name at any depth — varargs
 * JsonFilter f = DefaultJsonLogFilterBuilder.anonymizeKeys("password", "ssn");
 *
 * // Anonymize fields by name — Set with optional size limits
 * JsonFilter f = DefaultJsonLogFilterBuilder.anonymizeKeys(Set.of("password", "ssn"));
 * JsonFilter f = DefaultJsonLogFilterBuilder.anonymizeKeys(Set.of("password", "ssn"), 256);
 * JsonFilter f = DefaultJsonLogFilterBuilder.anonymizeKeys(Set.of("password", "ssn"), 256, 128 * 1024);
 *
 * // Anonymize fields by precise JSONPath — varargs
 * JsonFilter f = DefaultJsonLogFilterBuilder.anonymizePaths("$.customer.email");
 *
 * // Remove whole subtrees by field name — Set with optional size limits
 * JsonFilter f = DefaultJsonLogFilterBuilder.pruneKeys(Set.of("appMeta", "diagnostics"));
 * JsonFilter f = DefaultJsonLogFilterBuilder.pruneKeys(Set.of("appMeta"), 256, 128 * 1024);
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
