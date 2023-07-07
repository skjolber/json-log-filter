package com.github.skjolber.jsonfilter.test.truth;

import java.nio.file.Path;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.cache.JsonFileCache;
import com.github.skjolber.jsonfilter.test.directory.JsonFilterProperties;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparator;
import com.github.skjolber.jsonfilter.test.jackson.JsonNormalizer;

public class JsonFilterUnitTest {

	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private JsonFilter filter;
		
		private Path inputFile;
		private Path outputFile;
		private JsonFilterProperties filterProperties;
		private boolean literal = true;
		private JsonFilterMetrics metrics;
		private JsonFileCache jsonFileCache;
		private boolean whitespace = true;
		private boolean unicode = true;
		
		public Builder withMetrics(JsonFilterMetrics metrics) {
			this.metrics = metrics;
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

		public Builder withFilterProperties(JsonFilterProperties properties) {
			this.filterProperties = properties;
			return this;
		}

		public Builder withFilter(JsonFilter filter) {
			this.filter = filter;
			return this;
		}

		public Builder withLiteral(boolean enabled) {
			this.literal = enabled;
			return this;
		}		
		
		public Builder withUnicode(boolean enabled) {
			this.unicode = enabled;
			return this;
		}	

		public void isEqual() {
			if(inputFile == null) {
				throw new IllegalStateException();
			}
			if(outputFile == null) {
				throw new IllegalStateException();
			}
			if(filter == null) {
				throw new IllegalStateException();
			}
			if(filterProperties == null) {
				throw new IllegalStateException();
			}
			if(jsonFileCache == null) {
				jsonFileCache = JsonFileCache.getInstance();
			}

			String expectedJsonOutput = jsonFileCache.getFile(outputFile);
			
			JsonFilterSymmetryAssertion jsonFilterInputOutput = JsonFilterSymmetryAssertion.newInstance()
					.withFilter(filter)
					.withInputFile(inputFile)
					.withMetrics(metrics)
					.withUnicode(unicode)
					.withWhitespace(whitespace)
					.build();
			
			if(jsonFilterInputOutput == null) {
				return;
			}
			
			JsonFilterResult result = jsonFilterInputOutput.getResult();
			if(!result.hasStringOutput()) {
				throw new IllegalStateException();
			}
			if(!isEqual(expectedJsonOutput, result.getStringOutput())) {
				System.out.println(inputFile);
				System.out.println(outputFile);
				System.out.println(filterProperties.getProperties());
				System.out.println("         " + result.getStringInput());
				System.out.println("Expected " + expectedJsonOutput);
				System.out.println("Got      " + result.getStringOutput());

				throw new IllegalStateException();
			}
			
			return new JsonFilterUnitTest(filterProperties, result.getStringOutput(), jsonFilterInputOutput);
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

		public Builder withMetrics(DefaultJsonFilterMetrics metrics) {
			this.metrics = metrics;
			return this;
		}

		public Builder withWhitespace(boolean whitespace) {
			this.whitespace = whitespace;
			return this;
		}

	}

	private JsonFilterProperties outputProperties;
	private String output;
	private JsonFilterSymmetryAssertion inputOutput;
	
	public JsonFilterUnitTest(JsonFilterProperties outputProperties, String output, JsonFilterSymmetryAssertion inputOutput) {
		super();
		this.outputProperties = outputProperties;
		this.output = output;
		this.inputOutput = inputOutput;
	}
	
}
