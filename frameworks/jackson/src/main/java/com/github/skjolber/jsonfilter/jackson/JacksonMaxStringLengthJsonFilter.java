package com.github.skjolber.jsonfilter.jackson;
import java.io.IOException;

import org.apache.commons.io.output.StringBuilderWriter;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

public class JacksonMaxStringLengthJsonFilter extends AbstractJsonFilter implements JacksonJsonFilter {

	public static void writeMaxStringLength(final JsonParser parser, JsonGenerator generator, StringBuilder builder, int maxStringLength, char[] truncateStringValue)
			throws IOException {

		char[] textCharacters = parser.getTextCharacters();
		int textOffset = parser.getTextOffset();
		int textLength = parser.getTextLength();

		int keepLength = getTruncatedLength(textCharacters, textOffset, textLength, maxStringLength, truncateStringValue);
		if(keepLength != -1) {
			builder.append('"');
			quoteAsString(textCharacters, textOffset, textOffset + keepLength, builder);
			builder.append(truncateStringValue);
			builder.append(textLength - keepLength);
			builder.append('"');

			generator.writeRawValue(builder.toString());
			builder.setLength(0);
		} else {
			generator.writeString(textCharacters, textOffset, textLength);
		}
	}

	/**
	 * Number of characters to keep when truncating a string value longer than max string length. Used both
	 * for writing and for the size of what is written, so that the two agree.
	 *
	 * @param textCharacters characters of the string value
	 * @param textOffset offset of the string value
	 * @param textLength length of the string value, more than max string length
	 * @param maxStringLength max string length
	 * @param truncateStringValue truncate message
	 * @return characters to keep, or -1 if the value is kept as it is, as the truncate message and digits would not make it shorter
	 */

	public static int getTruncatedLength(char[] textCharacters, int textOffset, int textLength, int maxStringLength, char[] truncateStringValue) {
		// A high surrogate precedes a low surrogate.
		// check last include character

		int keepLength;
		if(Character.isLowSurrogate(textCharacters[textOffset + maxStringLength])) {
			keepLength = maxStringLength - 1;
		} else {
			keepLength = maxStringLength;
		}

		int removeLength = textLength - keepLength;

		// if truncate message + digits is smaller than the actual payload, trim it.
		if(removeLength - truncateStringValue.length - lengthToDigits(removeLength) > 0) {
			return keepLength;
		}
		return -1;
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

		int outputSize = output.length();
		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(chars, offset, length)
			) {
			if(process(parser, generator, metrics)) {
				if(metrics != null) {
					// writes buffered output and closes open structures, so the output size is final
					generator.close();
					metrics.onInput(length);
					metrics.onOutput(output.length() - outputSize);
				}
				return true;
			}
		} catch(final Exception e) {
			// fall through
		}
		// leave the output as it was
		output.setLength(outputSize);
		return false;
	}
	
	@Override
	public boolean process(byte[] bytes, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		if(bytes.length < offset + length) {
			return false;
		}
		output.ensureCapacity(output.length() + length);

		int outputSize = output.length();
		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			if(process(parser, generator, metrics)) {
				if(metrics != null) {
					// writes buffered output and closes open structures, so the output size is final
					generator.close();
					metrics.onInput(length);
					metrics.onOutput(output.length() - outputSize);
				}
				return true;
			}
		} catch(final Exception e) {
			// fall through
		}
		// leave the output as it was
		output.setLength(outputSize);
		return false;
	}

	public boolean process(byte[] bytes, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics metrics) {
		if(bytes.length < offset + length) {
			return false;
		}
		int outputSize = output.size();
		try (
			JsonGenerator generator = jsonFactory.createGenerator(output);
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			if(process(parser, generator, metrics)) {
				if(metrics != null) {
					// writes buffered output and closes open structures, so the output size is final
					generator.close();
					metrics.onInput(length);
					metrics.onOutput(output.size() - outputSize);
				}
				return true;
			}
		} catch(final Exception e) {
			// fall through
		}
		// leave the output as it was
		output.setSize(outputSize);
		return false;
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
				
				if(metrics != null) {
					metrics.onMaxStringLength(1);
				}
				
				continue;
			}
			JacksonEvents.copyCurrentEvent(parser, generator);
		}
		generator.flush(); // don't close

		return JacksonEvents.isSingleRootValue(parser);
	}


}