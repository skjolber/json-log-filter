package com.github.skjolber.jsonfilter.jackson;
import java.io.ByteArrayOutputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

public class DefaultJacksonJsonFilter extends AbstractJsonFilter {

	protected final JsonFactory jsonFactory;

	public DefaultJacksonJsonFilter() {
		this(new JsonFactory());
	}

	public DefaultJacksonJsonFilter(JsonFactory jsonFactory) {
		super(-1, -1, FILTER_PRUNE_MESSAGE, FILTER_ANONYMIZE, FILTER_TRUNCATE_MESSAGE);
		this.jsonFactory = jsonFactory;
	}
	
	protected DefaultJacksonJsonFilter(int maxStringLength, int maxSize, String pruneJson, String anonymizeJson, String truncateJsonString, JsonFactory jsonFactory) {
		super(maxStringLength, maxSize, pruneJson, anonymizeJson, truncateJsonString);
		
		this.jsonFactory = jsonFactory;
	}

	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		try (JsonParser parser = jsonFactory.createParser(chars, offset, length)) {
			if(parse(parser)) {
				output.ensureCapacity(output.length() + length);
				output.append(chars, offset, length);
				return true;
			}
			return false;
		} catch(final Exception e) {
			return false;
		}
	}

	@Override
	public boolean process(byte[] bytes, int offset, int length, ResizableByteArrayOutputStream output) {
		try (JsonParser parser = jsonFactory.createParser(bytes, offset, length)) {
			if(parse(parser)) {
				output.write(bytes, offset, length);
				
				return true;
			}
			return false;
		} catch(final Exception e) {
			return false;
		}		
	}

	protected boolean parse(JsonParser parser) {
		try {
			while(parser.nextToken() != null);
			
			return true;
		} catch(final Exception e) {
			return false;
		}
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

	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output,
			JsonFilterMetrics filterMetrics) {
		return process(chars, offset, length, output);
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output,
			JsonFilterMetrics filterMetrics) {
		return process(chars, offset, length, output);
	}
	
}