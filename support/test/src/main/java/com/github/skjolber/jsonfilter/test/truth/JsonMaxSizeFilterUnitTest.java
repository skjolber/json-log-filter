package com.github.skjolber.jsonfilter.test.truth;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.JsonFileCache;
import com.github.skjolber.jsonfilter.test.MaxSizeJsonFilterAdapter;
import com.github.skjolber.jsonfilter.test.directory.JsonFilterProperties;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparator;
import com.github.skjolber.jsonfilter.test.jackson.JsonNormalizer;

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
		private JsonFilterMetrics metrics;
		private boolean whitespace;
		private boolean unicode = true;

		public Builder withWhitespace(boolean whitespace) {
			this.whitespace = whitespace;
			return this;
		}
		
		public Builder withMetrics(JsonFilterMetrics metrics) {
			this.metrics = metrics;
			return this;
		}
		
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

		public Builder withUnicode(boolean unicode) {
			this.unicode  = unicode;
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
			
			JsonMaxSizeFilterInputOutput jsonFilterInputOutput = JsonMaxSizeFilterInputOutput.newBuilder()
					.withFilter(adapter)
					.withInputFile(inputFile)
					.withMetrics(metrics)
					.withUnicode(unicode)
					.withMinimumLengthChars(expectedJonOutput.length())
					.withMinimumLengthBytes(expectedJonOutput.getBytes(StandardCharsets.UTF_8).length)
					.withWhitespace(whitespace)
					.build();
			if(jsonFilterInputOutput == null) {
				return null;
			}
			
			JsonInputOutput result = jsonFilterInputOutput.getResult();
			if(!result.hasStringOutput()) {
				throw new IllegalStateException();
			}

			if(!isEqual(expectedJonOutput, result.getStringOutput())) {
				System.out.println("Unexpected result for " + inputFile);
				System.out.println("Input   :" + result.getStringInput());
				System.out.println("Output  :" + result.getStringOutput());
				System.out.println("Expected:" + expectedJonOutput);
				throw new IllegalStateException("Unexpected result for " + outputProperties.getProperties());
			}
			return new JsonMaxSizeFilterUnitTest(outputProperties, result.getStringOutput(), jsonFilterInputOutput.getFilter(), jsonFilterInputOutput.getResult());
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
	private JsonFilter filter;
	private JsonInputOutput jsonFilterResult;
	
	public JsonMaxSizeFilterUnitTest(JsonFilterProperties outputProperties, String output, JsonFilter filter, JsonInputOutput jsonFilterResult) {
		super();
		this.outputProperties = outputProperties;
		this.output = output;
		this.filter = filter;
		this.jsonFilterResult = jsonFilterResult;
	}
	
	
	
}
