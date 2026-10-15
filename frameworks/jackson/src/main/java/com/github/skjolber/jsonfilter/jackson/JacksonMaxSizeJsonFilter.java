package com.github.skjolber.jsonfilter.jackson;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.LongSupplier;

import org.apache.commons.io.output.StringBuilderWriter;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.TokenStreamContext;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

public class JacksonMaxSizeJsonFilter extends DefaultJacksonJsonFilter implements JacksonJsonFilter {

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
		super(-1, maxSize, pruneMessage, anonymizeMessage, truncateMessage, jsonFactory);
	}

	public boolean process(char[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		if(chars.length < offset + length) {
			return false;
		}
		if(maxSize >= length) {
			return super.process(chars, offset, length, output, metrics);
		}
		output.ensureCapacity(output.length() + length);

		int outputSize = output.length();
		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(chars, offset, length)
			) {
			if(process(parser, generator, () -> parser.currentLocation().getCharOffset(), () -> generator.streamWriteOutputBuffered() + output.length() - outputSize, metrics)) {
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
		if(maxSize >= length) {
			return super.process(bytes, offset, length, output, metrics);
		}

		int outputSize = output.size();
		try (
			JsonGenerator generator = jsonFactory.createGenerator(output);
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			if(process(parser, generator, () -> parser.currentLocation().getByteOffset(), () -> generator.streamWriteOutputBuffered() + output.size() - outputSize, metrics)) {
				return true;
			}
		} catch(final Exception e) {
			// fall through
		}
		// leave the output as it was
		output.setSize(outputSize);
		return false;
	}
	
	public boolean process(byte[] bytes, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		if(bytes.length < offset + length) {
			return false;
		}

		if(maxSize >= length) {
			return super.process(bytes, offset, length, output, metrics);
		}
		output.ensureCapacity(output.length() + length);

		int outputSize = output.length();
		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			if(process(parser, generator, () -> parser.currentLocation().getByteOffset(), () -> generator.streamWriteOutputBuffered() + output.length() - outputSize, metrics)) {
				return true;
			}
		} catch(final Exception e) {
			// fall through
		}
		// leave the output as it was
		output.setLength(outputSize);
		return false;
	}

	public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
		// estimate output size based on input size
		// if size limit is reached, do a more accurate output measurement
		
		// TODO optimize while output size < 50%, simplify copy operation

		int maxSize = this.maxSize;

		boolean utf8 = isUtf8(generator);

        String fieldName = null;
        
        long offset = offsetSupplier.getAsLong();
        
		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}

			switch(nextToken) {
			case START_ARRAY:
			case START_OBJECT:
				maxSize--;
				break;
			case END_ARRAY:
			case END_OBJECT:
				maxSize++;
				break;
			case PROPERTY_NAME:
				fieldName = parser.currentName();
				continue;
			case VALUE_STRING:
				parser.getTextLength();
				break;
				default: // do nothing
			}

			long nextOffset = offsetSupplier.getAsLong();
			
			long size = nextOffset - offset; // i.e. this includes whitespace
			
			long outputSize = outputSizeSupplier.getAsLong();

			if(outputSize + size > maxSize) {

				// do more accurate size calculation
				
				int accurateSize = getAccurateSize(parser, fieldName, nextToken, utf8);

				// a scalar root value is kept even if too big; dropping it would leave an empty (invalid) document
				if(outputSize + accurateSize > maxSize && !parser.streamReadContext().inRoot()) {
					if(metrics != null) {
						metrics.onMaxSize(-1);
					}
					
					break;
				}
			}

			if(fieldName != null) {
				generator.writeName(fieldName);
				fieldName = null;
			}

			JacksonEvents.copyCurrentEvent(parser, generator);
			
			offset = nextOffset;
		}
		generator.flush();
		
		return true;
	}

	protected static int getAccurateSize(final JsonParser parser, String fieldName, JsonToken nextToken) throws IOException {
		return getAccurateSize(parser, fieldName, nextToken, false);
	}

	/**
	 * Size of the current token as written by the generator, including separator and field name.
	 *
	 * @param parser parser at the current token
	 * @param fieldName pending field name, or null
	 * @param nextToken current token
	 * @param utf8 true if the output is UTF-8 bytes, false if characters
	 * @return size in bytes (UTF-8) or characters
	 * @throws IOException on parse error
	 */

	protected static int getAccurateSize(final JsonParser parser, String fieldName, JsonToken nextToken, boolean utf8) throws IOException {
		int accurateSize = getSeparatorSize(parser, nextToken);
		if(fieldName != null) {
			accurateSize += getQuotedSize(fieldName, utf8) + 1; // colon
		}
		
		switch(nextToken) {
		case START_ARRAY:
		case START_OBJECT:
		case END_ARRAY:
		case END_OBJECT: {
			accurateSize++;
			break;
		}
		case VALUE_STRING: {
			accurateSize += getQuotedSize(parser.getTextCharacters(), parser.getTextOffset(), parser.getTextLength(), utf8);
			break;
		}
		case VALUE_NUMBER_FLOAT:
		case VALUE_NUMBER_INT: {
			accurateSize += parser.getStringLength();
			break;
		}
		case VALUE_TRUE: 
		case VALUE_NULL: {
			accurateSize += 4;
			break;
		}
		case VALUE_FALSE: {
			accurateSize += 5;
			break;
		}
		default: // do nothing
		}
		return accurateSize;
	}

	/**
	 * Size of the separator (comma) the generator writes before the current token.
	 *
	 * @param parser parser at the current token
	 * @param token current token
	 * @return 1 if a comma is written, otherwise 0
	 */

	protected static int getSeparatorSize(final JsonParser parser, JsonToken token) {
		if(token.isStructEnd()) {
			return 0;
		}
		TokenStreamContext context = parser.streamReadContext();
		if(token.isStructStart()) {
			// the parser has already entered the new structure
			context = context.getParent();
		}
		if(context == null || context.inRoot()) {
			return 0;
		}
		return context.getCurrentIndex() >= 1 ? 1 : 0;
	}

	/**
	 * Size of a string value or field name as written by the generator: quotes, escapes and,
	 * for UTF-8 output, multi-byte characters.
	 *
	 * @param chars characters
	 * @param offset offset
	 * @param length length
	 * @param utf8 true if the output is UTF-8 bytes, false if characters
	 * @return size including quotes
	 */

	protected static int getQuotedSize(final char[] chars, int offset, int length, boolean utf8) {
		int size = length + 2;
		int limit = offset + length;
		for(int i = offset; i < limit; i++) {
			size += getExtraSize(chars[i], utf8);
		}
		return size;
	}

	protected static int getQuotedSize(final String string, boolean utf8) {
		int size = string.length() + 2;
		for(int i = 0; i < string.length(); i++) {
			size += getExtraSize(string.charAt(i), utf8);
		}
		return size;
	}

	/**
	 * Size of characters written as-is (i.e. a raw value), in characters or UTF-8 bytes.
	 *
	 * @param chars characters
	 * @param utf8 true if the output is UTF-8 bytes
	 * @return size
	 */

	protected static int getRawSize(final char[] chars, boolean utf8) {
		int size = chars.length;
		if(utf8) {
			for(int i = 0; i < chars.length; i++) {
				char c = chars[i];
				if(c >= 0x80) {
					size += c < 0x800 || Character.isSurrogate(c) ? 1 : 2;
				}
			}
		}
		return size;
	}

	private static int getExtraSize(char c, boolean utf8) {
		if(c < 0x20) {
			switch(c) {
			case '\b':
			case '\t':
			case '\n':
			case '\f':
			case '\r':
				return 1; // i.e. \n
			default:
				return 5; // i.e. \u0001
			}
		}
		if(c == '"' || c == '\\') {
			return 1;
		}
		if(utf8 && c >= 0x80) {
			// 2 bytes up to 0x7FF, 3 bytes otherwise, and 4 bytes for a surrogate pair (2 chars)
			return c < 0x800 || Character.isSurrogate(c) ? 1 : 2;
		}
		return 0;
	}

	/**
	 * Whether the generator writes UTF-8 bytes rather than characters.
	 *
	 * @param generator generator
	 * @return true if writing to an output stream
	 */

	protected static boolean isUtf8(JsonGenerator generator) {
		return generator.streamWriteOutputTarget() instanceof OutputStream;
	}

}