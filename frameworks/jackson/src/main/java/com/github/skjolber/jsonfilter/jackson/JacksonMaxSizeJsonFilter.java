package com.github.skjolber.jsonfilter.jackson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.output.StringBuilderWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

public class JacksonMaxSizeJsonFilter extends AbstractJsonFilter implements JacksonJsonFilter {

	protected final JsonFactory jsonFactory;
	
	public JacksonMaxSizeJsonFilter(int maxSize) {
		this(maxSize, new JsonFactory());
	}

	public JacksonMaxSizeJsonFilter(int maxSize, JsonFactory jsonFactory) {
		this(maxSize, FILTER_PRUNE_MESSAGE, FILTER_ANONYMIZE, FILTER_TRUNCATE_MESSAGE, jsonFactory);
	}

	public JacksonMaxSizeJsonFilter(int maxSize, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxSize, pruneMessage, anonymizeMessage, truncateMessage, new JsonFactory());
	}

	public JacksonMaxSizeJsonFilter(int maxSize, String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		super(-1, pruneMessage, anonymizeMessage, truncateMessage, maxSize);
		this.jsonFactory = jsonFactory;
	}
	
	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		output.ensureCapacity(output.length() + length);

		try (JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output))) {
			return process(chars, offset, length, generator);
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(byte[] bytes, int offset, int length, StringBuilder output) {
		output.ensureCapacity(output.length() + length);

		try (JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output))) {
			return process(bytes, offset, length, generator);
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(InputStream in, JsonGenerator generator) throws IOException {
		try (final JsonParser parser = jsonFactory.createParser(in)) {
			return process(parser, generator);
		}
	}

	public boolean process(byte[] bytes, int offset, int length, JsonGenerator generator) {
		try (final JsonParser parser = jsonFactory.createParser(bytes, offset, length)) {
			return process(parser, generator);
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(char[] chars, int offset, int length, JsonGenerator generator) {
		try (final JsonParser parser = jsonFactory.createParser(chars, offset, length)) {
			return process(parser, generator);
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(final JsonParser parser, JsonGenerator generator) throws IOException {
        boolean copyNext = false;
		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}
			
			if(!copyNext && parser.currentLocation().getCharOffset() >= maxSize) {
				break;
			}

			copyNext = nextToken == JsonToken.FIELD_NAME;
			
			generator.copyCurrentEvent(parser);
		}
		generator.flush(); // don't close

		return true;
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
		//output.ensureCapacity(output.length() + length);

		try (JsonGenerator generator = jsonFactory.createGenerator(output)) {
			return process(chars, offset, length, generator);
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
}