package com.github.skjolber.jsonfilter.test.truth;

import java.io.File;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.test.JsonFileCache;
import com.github.skjolber.jsonfilter.test.directory.JsonFilterProperties;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparator;
import com.github.skjolber.jsonfilter.test.jackson.JsonNormalizer;

public class JsonFilerUnitTest {

	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private JsonFilter filter;
		private File inputFile;
		private File outputFile;
		private JsonFilterProperties outputProperties;
		private boolean literal = true;
		
		public Builder withInputFile(File file) {
			this.inputFile = file;
			return this;
		}
		
		public Builder withOutputFile(File file) {
			this.outputFile = file;
			return this;
		}

		public Builder withOutputProperties(JsonFilterProperties properties) {
			this.outputProperties = properties;
			return this;
		}

		public Builder withFilter(JsonFilter filter) {
			this.filter = filter;
			return this;
		}
		
		public JsonFilerUnitTest build() {
			if(inputFile == null) {
				throw new IllegalStateException();
			}
			if(outputFile == null) {
				throw new IllegalStateException();
			}
			if(filter == null) {
				throw new IllegalStateException();
			}
			if(outputProperties == null) {
				throw new IllegalStateException();
			}

			String expectedJonOutput = JsonFileCache.getInstance().getFile(outputFile);
			JsonFilterInputOutput jsonFilterInputOutput = JsonFilterInputOutput.newBuilder().withFilter(filter).withInputFile(inputFile).build();
			JsonInputOutput result = jsonFilterInputOutput.getResult();
			if(result.hasStringOutput()) {
				throw new IllegalStateException();
			}

			if(!isEqual(expectedJonOutput, result.getStringOutput())) {
				throw new IllegalStateException();
			}
			return new JsonFilerUnitTest(outputProperties, result.getStringOutput(), jsonFilterInputOutput);
		}

		private boolean isEqual(String expectedJsonOutput, String stringOutput) {
			if(expectedJsonOutput.equals(stringOutput)) {
				return true;
			}

			String normalizedExpectedJsonOutput = JsonNormalizer.normalize(expectedJsonOutput);
			String normalizedStringOutput = JsonNormalizer.normalize(stringOutput); 

			if(normalizedExpectedJsonOutput.equals(normalizedStringOutput)) {
				return true;
			}

			if(literal) {
				return false;
			}

			return JsonComparator.isSameEvents(normalizedExpectedJsonOutput, normalizedStringOutput);
		}
	}

	private JsonFilterProperties outputProperties;
	private String output;
	private JsonFilterInputOutput inputOutput;
	
	public JsonFilerUnitTest(JsonFilterProperties outputProperties, String output, JsonFilterInputOutput inputOutput) {
		super();
		this.outputProperties = outputProperties;
		this.output = output;
		this.inputOutput = inputOutput;
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
