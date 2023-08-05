package com.github.skjolber.jsonfilter.test.truth;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.cache.JsonFile;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonCollection;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparator;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparisonType;

public class MaxSizeJsonFilterAssertion extends AbstractJsonFilterSymmetryAssertion {

	public static MaxSizeJsonFilterAssertion newInstance() {
		return new MaxSizeJsonFilterAssertion();
	}
	
	private JsonFile inputFile;
	private JsonFilterMetrics metrics;
	private MaxSizeJsonFilterPair maxSizeJsonFilterPair;

	public MaxSizeJsonFilterAssertion withMetrics(JsonFilterMetrics metrics) {
		this.metrics = metrics;
		return this;
	}
	
	public MaxSizeJsonFilterAssertion withMaxSizeJsonFilterPair(MaxSizeJsonFilterPair maxSizeJsonFilterPair) {
		this.maxSizeJsonFilterPair = maxSizeJsonFilterPair;
		return this;
	}
	
	public MaxSizeJsonFilterAssertion withInputFile(JsonFile file) {
		this.inputFile = file;
		return this;
	}

	public void filters(JsonFile outputFile) {
		filters(outputFile, JsonComparisonType.LITERAL);
	}
	
	public void filters(JsonFile outputFile, JsonComparisonType comparison) {
		if(inputFile == null) {
			throw new IllegalStateException();
		}
		if(maxSizeJsonFilterPair == null) {
			throw new IllegalStateException();
		}
		if(outputFile == null) {
			throw new IllegalStateException();
		}

		String inputContentAsString = inputFile.getContentAsString();
		byte[] inputContentAsBytes = inputFile.getContentAsBytes();

		String expectedOutputContentAsString = outputFile.getContentAsString();
		byte[] expectedOutputContentAsBytes = outputFile.getContentAsBytes();
		
		JsonFilter infiniteJsonFilter = maxSizeJsonFilterPair.getInfiniteJsonFilter();

		byte[] byteOutput = infiniteJsonFilter.process(inputContentAsBytes, metrics);
		String stringOutput = infiniteJsonFilter.process(inputContentAsString, metrics);
		
		if(comparison == JsonComparisonType.LITERAL) {
			assertEquals(inputFile.getSource(), inputContentAsString, stringOutput, expectedOutputContentAsString);
			assertEquals(inputFile.getSource(), inputContentAsBytes, byteOutput, expectedOutputContentAsBytes);
		} else {
			JsonComparator.assertEventsEqual(inputFile.getSource(), inputContentAsString, inputContentAsBytes, stringOutput, byteOutput);
		}
		
		List<MaxSizeJsonCollection> charsInputs = inputFile.getMaxSizeCollections();
		List<MaxSizeJsonCollection> byteInputs = inputFile.getMaxSizeCollections();

		List<MaxSizeJsonCollection> charsOutputs = outputFile.getMaxSizeCollections();
		List<MaxSizeJsonCollection> byteOutputs = outputFile.getMaxSizeCollections();

		for(int i = 0; i < charsInputs.size() - 1 && i < charsOutputs.size(); i++) {
			MaxSizeJsonCollection inputCurrent = charsInputs.get(i);
			MaxSizeJsonCollection inputNext = charsInputs.get(i + 1);

			MaxSizeJsonCollection outputCurrent = charsOutputs.get(i);

			// Check only the range that was added between items, i.e. when going from
			//
			// |-----------------------------------------|
			// | {"firstName":"John"}                    | Current
			// | {"firstName":"John","lastName":"Smith"} | Next
			// |                    ,"lastName":"Smith"  | In-scope range
			// |-----------------------------------------|
			
			int k = inputCurrent.getMark();
			while(k < inputNext.getMark()) {
				String charsValue = inputNext.getContentAsString();
				
				int maxByteSize = charsValue.substring(0, k).getBytes(StandardCharsets.UTF_8).length;
				int maxCharSize = k;

				String expectedMaxSizeCharsOutput = outputCurrent.getContentAsString();
				byte[] expectedMaxSizeBytesOutput = expectedMaxSizeCharsOutput.getBytes(StandardCharsets.UTF_8);

				maxCharSize = expectedMaxSizeCharsOutput.length();
				maxByteSize = expectedMaxSizeBytesOutput.length;

				/*
				if(expectedMaxSizeCharsOutput.length() > charsValue.length()) {
					
					// Output exceeds input
					//
					// |-----------------------------------------|
					// | ["a", "b"]                              | Input
					// | ["*****", "*****"]                      | Output
					// |-----------------------------------------|

					// add to the max limit to allow the filter to arrive at the same result
					maxByteSize = expectedMaxSizeCharsOutput.length();
					maxCharSize = expectedMaxSizeBytesOutput.length;
				} else if(expectedMaxSizeCharsOutput.length() > maxCharSize) {
					maxCharSize = expectedMaxSizeCharsOutput.length();
					maxByteSize = expectedMaxSizeBytesOutput.length;
				}
				*/

				byte[] bytesValue = charsValue.getBytes(StandardCharsets.UTF_8);
				
				JsonFilter bytesFilter = maxSizeJsonFilterPair.getMaxSizeJsonFilter(maxByteSize);
				JsonFilter charsFilter = maxSizeJsonFilterPair.getMaxSizeJsonFilter(maxCharSize);
				
				byte[] maxSizeBytesOutput = bytesFilter.process(bytesValue, metrics);
				String maxSizeCharsOutput = charsFilter.process(charsValue, metrics);
								
				System.out.println(maxByteSize + " " + maxCharSize);
				System.out.println(outputFile.getSource().toString());
				
				assertEquals(inputFile.getSource(), charsValue, maxSizeCharsOutput, expectedMaxSizeCharsOutput);
				assertEquals(inputFile.getSource(), bytesValue, maxSizeBytesOutput, expectedMaxSizeBytesOutput);
				
				// if the filter is removing whitespace, filter the input with various
				// variations of pretty-printing and compare to the output from the filtering
				// of the original input
				
				k = inputNext.nextCodePoint(k);
			}
		}
		
		
		for(int i = 0; i < charsInputs.size() - 1; i++) {
			MaxSizeJsonCollection charsInput = charsInputs.get(i);
			MaxSizeJsonCollection bytesInput = byteInputs.get(i);

			
			
			
		}		
	}


	
	
}
