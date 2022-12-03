package com.github.skjolber.jsonfilter.test.truth;

import java.nio.file.Path;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.test.JsonFileCache;
import com.github.skjolber.jsonfilter.test.MaxSizeJsonFilterAdapter;
import com.github.skjolber.jsonfilter.test.directory.JsonFilterProperties;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparator;
import com.github.skjolber.jsonfilter.test.jackson.JsonNormalizer;
import com.github.skjolber.jsonfilter.test.truth.JsonFilterUnitTest.Builder;

public class JsonMaxSizeFilterUnitTest {

	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private Path inputFile;
		private Path outputFile;
		private JsonFilterProperties outputProperties;
		private boolean literal = true;
		private MaxSizeJsonFilterAdapter adapter;
		private JsonFileCache cache = JsonFileCache.getInstance();
		
		public Builder withAdapter(MaxSizeJsonFilterAdapter adapter) {
			this.adapter = adapter;
			return this;
		}
		
		public Builder withLiteral(boolean enabled) {
			this.literal = enabled;
			return this;
		}
		
		public Builder withInputFile(Path file) {
			this.inputFile = file;
			return this;
		}

		public Builder withOutputFile(Path file) {
			this.outputFile = file;
			return this;
		}

		public Builder withOutputProperties(JsonFilterProperties properties) {
			this.outputProperties = properties;
			return this;
		}

		public JsonMaxSizeFilterUnitTest build() {
			if(inputFile == null) {
				throw new IllegalStateException();
			}
			if(outputFile == null) {
				throw new IllegalStateException();
			}
			if(outputProperties == null) {
				throw new IllegalStateException();
			}
			if(adapter == null) {
				throw new IllegalStateException();
			}

			String expectedJonOutput = cache.getFile(outputFile);
			
			JsonFilter filter = adapter.getMaxSize(expectedJonOutput.length());
			
			JsonFilterInputOutput jsonFilterInputOutput = JsonFilterInputOutput.newBuilder()
					.withFilter(filter)
					.withInputFile(inputFile)
					.withMinimumLength(expectedJonOutput.length())
					.build();
			JsonInputOutput result = jsonFilterInputOutput.getResult();
			if(!result.hasStringOutput()) {
				throw new IllegalStateException();
			}

			if(!isEqual(expectedJonOutput, result.getStringOutput())) {
				throw new IllegalStateException();
			}
			return new JsonMaxSizeFilterUnitTest(outputProperties, result.getStringOutput(), jsonFilterInputOutput);
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
	
	public JsonMaxSizeFilterUnitTest(JsonFilterProperties outputProperties, String output, JsonFilterInputOutput inputOutput) {
		super();
		this.outputProperties = outputProperties;
		this.output = output;
		this.inputOutput = inputOutput;
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
