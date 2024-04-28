package com.github.skjolber.jsonfilter.jackson;
import com.fasterxml.jackson.core.JsonFactory;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

public abstract class AbstractJacksonJsonFilter extends AbstractJsonFilter {

	protected final JsonFactory jsonFactory;

	public AbstractJacksonJsonFilter() {
		this(new JsonFactory());
	}

	public AbstractJacksonJsonFilter(JsonFactory jsonFactory) {
		super(-1, -1, FILTER_PRUNE_MESSAGE, FILTER_ANONYMIZE, FILTER_TRUNCATE_MESSAGE);
		this.jsonFactory = jsonFactory;
	}
	
	protected AbstractJacksonJsonFilter(int maxStringLength, int maxSize, String pruneJson, String anonymizeJson, String truncateJsonString, JsonFactory jsonFactory) {
		super(maxStringLength, maxSize, pruneJson, anonymizeJson, truncateJsonString);
		
		this.jsonFactory = jsonFactory;
	}

	protected char[] getPruneJsonValue() {
		return pruneJsonValue;
	}
	
	protected char[] getAnonymizeJsonValue() {
		return anonymizeJsonValue;
	}
	
	protected char[] getTruncateStringValue() {
		return truncateStringValue;
	}

}