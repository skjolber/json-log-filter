package com.github.skjolber.jsonfilter.jackson;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

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
}
