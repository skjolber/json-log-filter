package com.github.skjolber.jsonfilter.test.truth;

import java.util.List;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.cache.JsonFile;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonCollection;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparator;

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

	public void isEqualTo(JsonFile outputFile) {
		isEqualTo(outputFile, true);
	}
	

	public void isJsonEventsEqualTo(JsonFile outputFile) {
		isEqualTo(outputFile, false);
	}
	
	private void isEqualTo(JsonFile outputFile, boolean literal) {
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
		
		if(literal) {
			assertEquals(inputFile.getSource(), inputContentAsString, stringOutput, expectedOutputContentAsString);
			assertEquals(inputFile.getSource(), inputContentAsBytes, byteOutput, expectedOutputContentAsBytes);
		} else {
			JsonComparator.assertEventsEqual(inputFile.getSource(), inputContentAsString, inputContentAsBytes, stringOutput, byteOutput);
		}
		
		List<MaxSizeJsonCollection> charsInputs = inputFile.getMaxSizeCollections();
		List<MaxSizeJsonCollection> byteInputs = outputFile.getMaxSizeCollections();
			
		for(int i = 0; i < charsInputs.size() - 1; i++) {
			MaxSizeJsonCollection charsInput = charsInputs.get(i);
			MaxSizeJsonCollection bytesInput = byteInputs.get(i);

			
			
			
		}		
	}


	
	
}
