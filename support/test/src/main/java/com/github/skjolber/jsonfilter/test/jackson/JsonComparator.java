package com.github.skjolber.jsonfilter.test.jackson;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class JsonComparator {

	/*
	
							System.out.println();
							System.out.println(charsValue);
							System.out.println(new String(byteOutput2, StandardCharsets.UTF_8));
							System.out.println(stringOutput2);
							fail("Expected event symmertic pretty-printed result for " + input.getSource() + " max size " + k);
	
	
	*/
	
	public static void assertEventsEqual(Path source, String input, String actualOutput, String expectedOutput) {
		assertEventsEqual(source, input, input, actualOutput, expectedOutput);
	}

	public static void assertEventsEqual(Path source, byte[] input, byte[] actualOutput, byte[] expectedOutput) {
		assertEventsEqual(source, 
				new String(input, StandardCharsets.UTF_8),
				new String(actualOutput, StandardCharsets.UTF_8), 
				new String(expectedOutput, StandardCharsets.UTF_8)
			);
	}

	public static void assertEventsEqual(Path path, String input1, byte[] input2, String output1, byte[] output2) {
		assertEventsEqual(path, input1, new String(input2, StandardCharsets.UTF_8), output1, new String(output2, StandardCharsets.UTF_8));
	}

	public static void assertEventsEqual(Path path, String input1, String input2, String output1, String output2) {
		if(!isEventsEqual(output1, output2)) {
			StringBuilder builder = new StringBuilder();
			if(input1.equals(input2)) {
				builder.append("Expected equal result\n");
				builder.append(input1);
				builder.append("\n");
			} else {
				builder.append("Expected equal symmertic result\n");
				
				builder.append(input1);
				builder.append("\n");
				builder.append(input2);
				builder.append("\n");
			}
			
			builder.append(JsonComparator.printDiff(output1, output2));
			
			Assertions.fail(builder.toString());
		}
	}

	public static boolean isEventsEqual(String expected, String result) {
		JsonFactory jsonFactory = new JsonFactory();
		
		try (
				JsonParser expectedParser = jsonFactory.createParser(expected);
				JsonParser resultParser = jsonFactory.createParser(result)) {

			while(true) {
				JsonToken expectedToken = expectedParser.nextToken();
				JsonToken resultToken = resultParser.nextToken();
				if(expectedToken == null) {
					if(resultToken != null) {
						return false;
					}
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
					if(resultToken != null) {
						return false;
					}
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
	
	public static String printDiff(CharSequence result, CharSequence expected) {
		StringBuilder builder = new StringBuilder();
		if(expected.length() != result.length()) {
			builder.append("Expected size " + expected.length() + ", got " + result.length());
			builder.append("\n");
		}
		
		String compactResult = JsonCompactor.compact(result);
		String compactExpected = JsonCompactor.compact(expected);

		builder.append("Expected vs actual:\n");
		builder.append(compactExpected);
		builder.append("\n");
		builder.append(compactResult);
		builder.append("\n");

		for(int k = 0; k < Math.min(compactResult.length(), compactExpected.length()); k++) {
			if(expected.charAt(k) != result.charAt(k)) {
				builder.append("^");

				break;
			}
			builder.append(" ");
		}
		builder.append("\n");
		
		return builder.toString();
	}

	public static String printDiff(byte[] result, byte[] expected) {
		StringBuilder builder = new StringBuilder();
		
		if(expected.length != result.length) {
			builder.append("Expected size " + expected.length + ", got " + result.length );
			builder.append("\n");
		}
		
		byte[] compactResult = JsonCompactor.compact(result);
		byte[] compactExpected = JsonCompactor.compact(expected);
		
		builder.append("Expected vs actual:\n");
		builder.append(new String(compactExpected, StandardCharsets.UTF_8));
		builder.append("\n");
		builder.append(new String(compactResult, StandardCharsets.UTF_8));
		builder.append("\n");

		for(int k = 0; k < Math.min(compactResult.length, compactExpected.length); k++) {
			if(expected[k] != result[k]) {
				builder.append("^");

				break;
			}
			builder.append(" ");
		}
		builder.append("\n");
		return builder.toString();
	}


}
