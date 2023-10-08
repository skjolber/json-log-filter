package com.github.skjolber.jsonfilter.jackson;
import java.io.IOException;

import org.apache.commons.io.output.StringBuilderWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

public class JacksonMaxStringLengthJsonFilter extends AbstractJsonFilter implements JacksonJsonFilter {

	public static void writeMaxStringLength(final JsonParser parser, JsonGenerator generator, StringBuilder builder, int maxStringLength, char[] truncateStringValue)
			throws IOException {
		
		char[] textCharacters = parser.getTextCharacters();
		int textOffset = parser.getTextOffset();
		
		// A high surrogate precedes a low surrogate.
		// check last include character

		int keepLength;
		if(Character.isLowSurrogate(textCharacters[textOffset + maxStringLength])) {
			keepLength = maxStringLength - 1;
		} else {
			keepLength = maxStringLength;
		}
		
		int removeLength = parser.getTextLength() - keepLength;

		// if truncate message + digits is smaller than the actual payload, trim it.
		int actualReductionLength = removeLength - truncateStringValue.length - lengthToDigits(removeLength);
		if(actualReductionLength > 0) {
			builder.append('"');
			quoteAsString(textCharacters, textOffset, textOffset + keepLength, builder);
			builder.append(truncateStringValue);
			builder.append(removeLength);
			builder.append('"');
			
			generator.writeRawValue(builder.toString());
			builder.setLength(0);
		} else {
			generator.writeString(textCharacters, textOffset, parser.getTextLength());
		}
	}
	
	public static void skipMaxStringLength(final JsonParser parser, JsonGenerator generator, int maxStringLength, StringBuilder builder, JsonFilterMetrics metrics, char[] truncateStringValue) throws IOException {
		int level = 1;
		
		while(level > 0) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}
			
			switch(nextToken) {
			case START_OBJECT:
			case START_ARRAY:
				level++;
				break;
			case END_OBJECT:
			case END_ARRAY:
				level--;
				break;
			case VALUE_STRING:
				if(parser.getTextLength() > maxStringLength) {
					writeMaxStringLength(parser, generator, builder, maxStringLength, truncateStringValue);
					
					if(metrics != null) {
						metrics.onMaxStringLength(1);
					}
					
					continue;
				}
			}
			
			generator.copyCurrentEvent(parser);
		}
	}
	
	protected final JsonFactory jsonFactory;

	public JacksonMaxStringLengthJsonFilter(int maxStringLength) {
		this(maxStringLength, new JsonFactory());
	}

	public JacksonMaxStringLengthJsonFilter(int maxStringLength, JsonFactory jsonFactory) {
		this(maxStringLength, FILTER_PRUNE_MESSAGE, FILTER_ANONYMIZE, FILTER_TRUNCATE_MESSAGE, jsonFactory);
	}

	public JacksonMaxStringLengthJsonFilter(int maxStringLength, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, pruneMessage, anonymizeMessage, truncateMessage, new JsonFactory());
	}

	public JacksonMaxStringLengthJsonFilter(int maxStringLength, String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		this(maxStringLength, -1, pruneMessage, anonymizeMessage, truncateMessage, jsonFactory);
	}
	
	public JacksonMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String pruneJson, String anonymizeJson, String truncateJsonString, JsonFactory jsonFactory) {
		super(maxStringLength, maxSize, pruneJson, anonymizeJson, truncateJsonString);
		this.jsonFactory = jsonFactory;
	}

	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		if(chars.length < offset + length) {
			return false;
		}
		output.ensureCapacity(output.length() + length);

		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(chars, offset, length)
			) {
			return process(parser, generator);
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(byte[] bytes, int offset, int length, StringBuilder output) {
		if(bytes.length < offset + length) {
			return false;
		}
		output.ensureCapacity(output.length() + length);

		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			return process(parser, generator);
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(byte[] bytes, int offset, int length, ResizableByteArrayOutputStream output) {
		if(bytes.length < offset + length) {
			return false;
		}
		try (
			JsonGenerator generator = jsonFactory.createGenerator(output);
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			return process(parser, generator);
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(final JsonParser parser, JsonGenerator generator) throws IOException {
		StringBuilder builder = new StringBuilder(Math.max(16 * 1024, maxStringLength + 11 + truncateStringValue.length + 2)); // i.e

		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}
			
			if(nextToken == JsonToken.VALUE_STRING && parser.getTextLength() > maxStringLength) {
				writeMaxStringLength(parser, generator, builder, maxStringLength, truncateStringValue);
				continue;
			}
			generator.copyCurrentEvent(parser);
		}
		generator.flush(); // don't close

		return true;
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

	public boolean process(char[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		if(chars.length < offset + length) {
			return false;
		}
		output.ensureCapacity(output.length() + length);

		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(chars, offset, length)
			) {
			return process(parser, generator, metrics);
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(byte[] bytes, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		if(bytes.length < offset + length) {
			return false;
		}
		output.ensureCapacity(output.length() + length);

		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			return process(parser, generator, metrics);
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(byte[] bytes, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics metrics) {
		if(bytes.length < offset + length) {
			return false;
		}
		try (
			JsonGenerator generator = jsonFactory.createGenerator(output);
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			return process(parser, generator, metrics);
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) throws IOException {
		StringBuilder builder = new StringBuilder(Math.max(16 * 1024, maxStringLength + 11 + truncateStringValue.length + 2)); // i.e

		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}
			
			if(nextToken == JsonToken.VALUE_STRING && parser.getTextLength() > maxStringLength) {
				writeMaxStringLength(parser, generator, builder, maxStringLength, truncateStringValue);
				
				metrics.onMaxStringLength(1);
				
				continue;
			}
			generator.copyCurrentEvent(parser);
		}
		generator.flush(); // don't close

		return true;
	}


}