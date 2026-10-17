package com.github.skjolber.jsonfilter.jackson;

import java.io.IOException;
import java.io.OutputStream;

import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.TokenStreamContext;

/**
 * Size of what the generator writes, in characters or UTF-8 bytes, so that the max size filters can check
 * a token before writing it.
 */

final class JacksonOutputSize {

	private JacksonOutputSize() {
	}

	/**
	 * Whether the generator writes UTF-8 bytes rather than characters.
	 *
	 * @param generator generator
	 * @return true if writing to an output stream
	 */

	static boolean isUtf8(JsonGenerator generator) {
		return generator.streamWriteOutputTarget() instanceof OutputStream;
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

	static int getAccurateSize(final JsonParser parser, String fieldName, JsonToken nextToken, boolean utf8) throws IOException {
		int accurateSize = getPrefixSize(parser, nextToken, fieldName, utf8);

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
	 * Size of the current token as written by the generator, including separator and field name, and
	 * truncation of long string values like {@link JacksonMaxStringLengthJsonFilter#writeMaxStringLength}.
	 *
	 * @param parser parser at the current token
	 * @param fieldName pending field name, or null
	 * @param nextToken current token
	 * @param maxStringLength max string length
	 * @param truncateStringValue truncate message
	 * @param truncateSize size of the truncate message, in bytes (UTF-8) or characters
	 * @param utf8 true if the output is UTF-8 bytes, false if characters
	 * @return size in bytes (UTF-8) or characters
	 * @throws IOException on parse error
	 */

	static int getAccurateMaxStringSize(final JsonParser parser, String fieldName, JsonToken nextToken, int maxStringLength, char[] truncateStringValue, int truncateSize,
			boolean utf8) throws IOException {
		if(nextToken != JsonToken.VALUE_STRING || parser.getTextLength() <= maxStringLength) {
			return getAccurateSize(parser, fieldName, nextToken, utf8);
		}

		char[] textCharacters = parser.getTextCharacters();
		int textOffset = parser.getTextOffset();
		int textLength = parser.getTextLength();

		int size = getPrefixSize(parser, nextToken, fieldName, utf8);

		int keepLength = JacksonMaxStringLengthJsonFilter.getTruncatedLength(textCharacters, textOffset, textLength, maxStringLength, truncateStringValue);
		if(keepLength == -1) {
			return size + getQuotedSize(textCharacters, textOffset, textLength, utf8);
		}
		return size + getQuotedSize(textCharacters, textOffset, keepLength, utf8) + truncateSize + AbstractJsonFilter.lengthToDigits(textLength - keepLength);
	}

	/**
	 * Size of what the generator writes before the value of the current token: the separator (comma), and
	 * the field name and colon, if any.
	 *
	 * @param parser parser at the current token
	 * @param token current token
	 * @param fieldName pending field name, or null
	 * @param utf8 true if the output is UTF-8 bytes, false if characters
	 * @return size in bytes (UTF-8) or characters
	 */

	static int getPrefixSize(final JsonParser parser, JsonToken token, String fieldName, boolean utf8) {
		int size = getSeparatorSize(parser, token);
		if(fieldName != null) {
			size += getQuotedSize(fieldName, utf8) + 1; // colon
		}
		return size;
	}

	/**
	 * Size of the separator (comma) the generator writes before the current token.
	 *
	 * @param parser parser at the current token
	 * @param token current token
	 * @return 1 if a comma is written, otherwise 0
	 */

	static int getSeparatorSize(final JsonParser parser, JsonToken token) {
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

	static int getQuotedSize(final char[] chars, int offset, int length, boolean utf8) {
		int size = length + 2;
		int limit = offset + length;
		for(int i = offset; i < limit; i++) {
			size += getExtraSize(chars[i], utf8);
		}
		return size;
	}

	static int getQuotedSize(final String string, boolean utf8) {
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

	static int getRawSize(final char[] chars, boolean utf8) {
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
				return 1; // i.e. backslash and letter
			default:
				return 5; // i.e. backslash, u and four hex digits
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
	 * Size of the input left out by max size, for the max size metric.
	 *
	 * @param inputLength input length in characters or bytes, or -1 if unknown
	 * @param offset offset of the end of the last input written, relative to the start of the input
	 * @return input left out, or -1 if unknown
	 */

	static int getLeftOutSize(int inputLength, long offset) {
		return inputLength == -1 ? -1 : (int)(inputLength - offset);
	}

	/**
	 * Start of the current token, in the input's characters or bytes.
	 *
	 * @param parser parser
	 * @return offset relative to the start of the input
	 */

	static long getTokenStart(JsonParser parser) {
		TokenStreamLocation location = parser.currentTokenLocation();
		long byteOffset = location.getByteOffset();
		return byteOffset != -1 ? byteOffset : location.getCharOffset();
	}

}
