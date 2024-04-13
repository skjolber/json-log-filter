package com.github.skjolber.jsonfilter.test.truth;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.cache.JsonFile;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparator;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparisonType;

public class JsonFilterAssertion extends AbstractJsonFilterSymmetryAssertion {

	public static JsonFilterAssertion newBuilder() {
		return new JsonFilterAssertion();
	}
	
	private JsonFilters filters;
	private JsonFile inputFile;
	private JsonFilterMetrics metrics;
	
	public JsonFilterAssertion withMetrics(JsonFilterMetrics metrics) {
		this.metrics = metrics;
		return this;
	}
	
	public JsonFilterAssertion withInputFile(JsonFile file) {
		this.inputFile = file;
		return this;
	}

	public JsonFilterAssertion withFilter(JsonFilter filter) {
		this.filters = new JsonFilters(filter);
		return this;
	}

	public JsonFilterAssertion withFilters(JsonFilters filters) {
		this.filters = filters;
		return this;
	}

	public JsonFilterAssertion withMetrics(DefaultJsonFilterMetrics metrics) {
		this.metrics = metrics;
		return this;
	}
	
	public void isJsonEventsEqualTo(JsonFile outputFile) {
		isEqualTo(outputFile, JsonComparisonType.LITERAL);
	}
	
	public void isEqualTo(JsonFile outputFile, JsonComparisonType comparison) {
		if(inputFile == null) {
			throw new IllegalStateException();
		}
		if(filters == null) {
			throw new IllegalStateException();
		}
		if(outputFile == null) {
			throw new IllegalStateException();
		}

		String inputContentAsString = inputFile.getContentAsString();
		byte[] inputContentAsBytes = inputFile.getContentAsBytes();

		String expectedOutputContentAsString = outputFile.getContentAsString();
		byte[] expectedOutputContentAsBytes = outputFile.getContentAsBytes();
		
		byte[] byteOutput = filters.getBytes().process(inputContentAsBytes, metrics);
		String stringOutput = filters.getCharacters().process(inputContentAsString, metrics);
		
		if(comparison == JsonComparisonType.LITERAL) {
			assertEquals(inputFile.getSource(), inputContentAsString, stringOutput, expectedOutputContentAsString);
			assertEquals(inputFile.getSource(), inputContentAsBytes, byteOutput, expectedOutputContentAsBytes);
		} else {
			JsonComparator.assertEventsEqual(inputFile.getSource(), inputContentAsString, stringOutput, expectedOutputContentAsString);
			JsonComparator.assertEventsEqual(inputFile.getSource(), inputContentAsBytes, byteOutput, expectedOutputContentAsBytes);
		}
		
		if(filters.getCharacters().isRemovingWhitespace()) {
			// compare each pretty-printed input against the same output as the non-pretty-printed input matched.
			for(int i = 0; i < inputFile.getPrettyPrintedSize(); i++) {
				String prettyPrintedAsString = inputFile.getPrettyPrintedAsString(i);

				stringOutput = filters.getCharacters().process(prettyPrintedAsString, metrics);
				
				if(comparison == JsonComparisonType.LITERAL) {
					assertEquals(inputFile.getSource(), inputContentAsString, prettyPrintedAsString, stringOutput, expectedOutputContentAsString);
				} else {
					JsonComparator.assertEventsEqual(inputFile.getSource(), inputContentAsString, stringOutput, expectedOutputContentAsString);
				}
			}
		} else {
			// compare each pretty-printed input against the correspondingly pretty-printed output
			for(int i = 0; i < inputFile.getPrettyPrintedSize(); i++) {
				String prettyPrintedInputAsString = inputFile.getPrettyPrintedAsString(i);

				String expectedPrettyPrintedOutputAsString = outputFile.getPrettyPrintedAsString(i);

				stringOutput = filters.getCharacters().process(prettyPrintedInputAsString, metrics);
				
				if(comparison == JsonComparisonType.LITERAL) {
					assertEquals(inputFile.getSource(), prettyPrintedInputAsString, stringOutput, expectedPrettyPrintedOutputAsString);
				} else {
					JsonComparator.assertEventsEqual(inputFile.getSource(), prettyPrintedInputAsString, stringOutput, expectedPrettyPrintedOutputAsString);
				}
			}
		}
		
		if(filters.getBytes().isRemovingWhitespace()) {
			// compare each pretty-printed input against the same output as the non-pretty-printed input matched.
			for(int i = 0; i < inputFile.getPrettyPrintedSize(); i++) {
				byte[] prettyPrintedAsBytes = inputFile.getPrettyPrintedAsBytes(i);

				byteOutput = filters.getBytes().process(prettyPrintedAsBytes, metrics);
				
				if(comparison == JsonComparisonType.LITERAL) {
					assertEquals(inputFile.getSource(), inputContentAsBytes, prettyPrintedAsBytes, byteOutput, expectedOutputContentAsBytes);
				} else {
					JsonComparator.assertEventsEqual(inputFile.getSource(), inputContentAsBytes, byteOutput, expectedOutputContentAsBytes);
				}
			}
		} else {
			// compare each pretty-printed input against the correspondingly pretty-printed output
			for(int i = 0; i < inputFile.getPrettyPrintedSize(); i++) {
				byte[] prettyPrintedInputAsBytes = inputFile.getPrettyPrintedAsBytes(i);

				byte[] expectedPrettyPrintedOutputAsBytes = outputFile.getPrettyPrintedAsBytes(i);

				byteOutput = filters.getBytes().process(prettyPrintedInputAsBytes, metrics);
				
				if(comparison == JsonComparisonType.LITERAL) {
					assertEquals(inputFile.getSource(), prettyPrintedInputAsBytes, byteOutput, expectedPrettyPrintedOutputAsBytes);
				} else {
					JsonComparator.assertEventsEqual(inputFile.getSource(), prettyPrintedInputAsBytes, byteOutput, expectedPrettyPrintedOutputAsBytes);
				}
			}
		}
	}

}
