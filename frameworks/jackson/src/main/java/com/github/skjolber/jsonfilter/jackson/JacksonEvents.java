package com.github.skjolber.jsonfilter.jackson;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.TokenStreamContext;

/**
 * Helper for copying parser events to a generator without altering number values.
 */

final class JacksonEvents {

	private JacksonEvents() {
	}

	/**
	 * Copy the current parser event to the generator. Numbers are written exactly as they
	 * appear in the input; {@linkplain JsonGenerator#copyCurrentEvent(JsonParser)} converts
	 * floating point numbers via double, which loses precision and turns out-of-range values
	 * like 1e400 into "Infinity".
	 *
	 * @param parser parser positioned at the event to copy
	 * @param generator generator to write to
	 */

	static void copyCurrentEvent(JsonParser parser, JsonGenerator generator) {
		JsonToken token = parser.currentToken();
		if(token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
			generator.writeNumber(parser.getStringCharacters(), parser.getStringOffset(), parser.getStringLength());
		} else {
			generator.copyCurrentEvent(parser);
		}
	}

	/**
	 * Check that no more than one root-level value was read, i.e. that a document like
	 * <code>{"a":1}{"b":2}</code> is rejected. Call this once after reading the tokens,
	 * so that there is no cost per token.
	 *
	 * @param parser parser which has read the tokens
	 * @return true if no more than one root-level value was read
	 */

	static boolean isSingleRootValue(JsonParser parser) {
		TokenStreamContext context = parser.streamReadContext();
		while(!context.inRoot()) {
			// stopped within a value, i.e. at max size
			context = context.getParent();
		}
		return context.getEntryCount() <= 1;
	}
}
