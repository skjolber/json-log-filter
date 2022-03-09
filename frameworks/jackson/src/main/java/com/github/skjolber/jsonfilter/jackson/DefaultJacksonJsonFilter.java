package com.github.skjolber.jsonfilter.jackson;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
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
	
	public boolean process(byte[] bytes, int offset, int length, StringBuilder output) {
		try (JsonParser parser = jsonFactory.createParser(bytes, offset, length)) {
			if(parse(parser)) {
				output.ensureCapacity(output.length() + length);
				
				char[] buffer = new char[4 * 1024];
				
				InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(bytes, offset, length), StandardCharsets.UTF_8);

				int read;
				do {
					read = isr.read(buffer);
					if(read == -1) {
						break;
					}
					output.append(buffer, 0, read);
				} while(true);
				
				return true;
			}
			return false;
		} catch(final Exception e) {
			return false;
		}
		
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
		try (JsonParser parser = jsonFactory.createParser(chars, offset, length)) {
			if(parse(parser)) {
				output.write(chars, offset, length);
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
}