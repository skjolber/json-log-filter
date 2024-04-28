package com.github.skjolber.jsonfilter.jackson;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

public class DefaultJacksonJsonFilter extends AbstractJacksonJsonFilter {

	public DefaultJacksonJsonFilter() {
		super();
	}

	public DefaultJacksonJsonFilter(JsonFactory jsonFactory) {
		super(jsonFactory);
	}
	
	protected DefaultJacksonJsonFilter(int maxStringLength, int maxSize, String pruneJson, String anonymizeJson, String truncateJsonString, JsonFactory jsonFactory) {
		super(maxStringLength, maxSize, pruneJson, anonymizeJson, truncateJsonString, jsonFactory);
	}

	public boolean process(char[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics filterMetrics) {
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
	public boolean process(byte[] bytes, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics filterMetrics) {
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
		
}