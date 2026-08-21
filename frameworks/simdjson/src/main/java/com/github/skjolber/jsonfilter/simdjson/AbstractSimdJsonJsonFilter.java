package com.github.skjolber.jsonfilter.simdjson;

import tools.jackson.core.json.JsonFactory;

import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

public abstract class AbstractSimdJsonJsonFilter extends AbstractJsonFilter {

	protected final JsonFactory jsonFactory;

	protected AbstractSimdJsonJsonFilter(int maxStringLength, int maxSize, String pruneJson, String anonymizeJson, String truncateJsonString) {
		this(maxStringLength, maxSize, pruneJson, anonymizeJson, truncateJsonString, new JsonFactory());
	}

	protected AbstractSimdJsonJsonFilter(int maxStringLength, int maxSize, String pruneJson, String anonymizeJson, String truncateJsonString, JsonFactory jsonFactory) {
		super(maxStringLength, maxSize, pruneJson, anonymizeJson, truncateJsonString);
		this.jsonFactory = jsonFactory;
	}

}
