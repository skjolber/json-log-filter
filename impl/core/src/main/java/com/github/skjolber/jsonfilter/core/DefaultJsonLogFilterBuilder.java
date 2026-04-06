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
 * <pre>{@code
 * JsonFilter filter = DefaultJsonLogFilterBuilder.newBuilder()
 *     .withMaxStringLength(127)
 *     .withAnonymize("$.customer.email", "$.customer.ssn")
 *     .withPrune("$.internal.debug", "$.internal.trace")
 *     .withAnonymizeMessage("[redacted]")
 *     .withPruneMessage("[removed]")
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
