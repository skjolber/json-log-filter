package com.github.skjolber.jsonfilter.test.jackson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class JsonComparator {

	public static boolean isSameEvents(String expected, String result) {
		JsonFactory jsonFactory = new JsonFactory();
		
		try (
				JsonParser expectedParser = jsonFactory.createParser(expected);
				JsonParser resultParser = jsonFactory.createParser(result)) {

			while(true) {
				JsonToken expectedToken = expectedParser.nextToken();
				JsonToken resultToken = resultParser.nextToken();
				if(expectedToken == null) {
					break;
				}

				if(!expectedToken.equals(resultToken)) {
					return false;
				}
				
				switch(expectedToken)  {
					case FIELD_NAME: {
						if(!expectedParser.getCurrentName().equals(resultParser.getCurrentName())) {
							return false;
						}
						break;
					}
					case VALUE_FALSE:
					case VALUE_TRUE: {
						if(expectedParser.getBooleanValue() != resultParser.getBooleanValue()) {
							return false;
						}
						break;
					}
					case VALUE_STRING: {
						if(!expectedParser.getText().equals(resultParser.getText())) {
							return false;
						}
						break;
					}
					case VALUE_NUMBER_INT: {
						if(expectedParser.getIntValue() != resultParser.getIntValue()) {
							return false;
						}
						break;
					}
					case VALUE_NUMBER_FLOAT: {
						if(expectedParser.getFloatValue() != resultParser.getFloatValue()) {
							return false;
						}
						break;
					}
					case VALUE_NULL: {
						break;
					}
				}
			}
		} catch(Exception e) {
			return false;
		}

		return true;
	}

	public static boolean isSameEvents(byte[] expected, byte[] result) {
		JsonFactory jsonFactory = new JsonFactory();
		
		try (
				JsonParser expectedParser = jsonFactory.createParser(expected);
				JsonParser resultParser = jsonFactory.createParser(result)) {

			while(true) {
				JsonToken expectedToken = expectedParser.nextToken();
				JsonToken resultToken = resultParser.nextToken();
				if(expectedToken == null) {
					break;
				}

				if(!expectedToken.equals(resultToken)) {
					return false;
				}
				
				switch(expectedToken)  {
					case FIELD_NAME: {
						if(!expectedParser.getCurrentName().equals(resultParser.getCurrentName())) {
							return false;
						}
						break;
					}
					case VALUE_FALSE:
					case VALUE_TRUE: {
						if(expectedParser.getBooleanValue() != resultParser.getBooleanValue()) {
							return false;
						}
						break;
					}
					case VALUE_STRING: {
						if(!expectedParser.getText().equals(resultParser.getText())) {
							return false;
						}
						break;
					}
					case VALUE_NUMBER_INT: {
						if(expectedParser.getIntValue() != resultParser.getIntValue()) {
							return false;
						}
						break;
					}
					case VALUE_NUMBER_FLOAT: {
						if(expectedParser.getFloatValue() != resultParser.getFloatValue()) {
							return false;
						}
						break;
					}
					case VALUE_NULL: {
						break;
					}
				}
			}
		} catch(Exception e) {
			return false;
		}

		return true;
	}

}
