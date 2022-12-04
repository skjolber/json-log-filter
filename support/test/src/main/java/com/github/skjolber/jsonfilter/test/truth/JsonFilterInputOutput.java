package com.github.skjolber.jsonfilter.test.truth;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparator;
import com.github.skjolber.jsonfilter.test.jackson.JsonNormalizer;

public class JsonFilterInputOutput {

	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private JsonFilter filter;
		private Path inputFile;
		
		// add padding so that the max length code paths are in use
		private int minimumLength = -1;
		private JsonCache jsonCache = JsonCache.getInstance();
		private JsonFilterMetrics metrics;
		
		public Builder withMinimumLength(int length) {
			this.minimumLength = length;
			return this;
		}
		
		public Builder withInputFile(Path file) {
			this.inputFile = file;
			return this;
		}
		
		public Builder withMetrics(JsonFilterMetrics metrics) {
			this.metrics = metrics;
			return this;
		}
		
		public Builder withFilter(JsonFilter filter) {
			this.filter = filter;
			return this;
		}
		
		public JsonFilterInputOutput build() {
			if(inputFile == null) {
				throw new IllegalStateException();
			}
			if(filter == null) {
				throw new IllegalStateException();
			}
			if(metrics == null) {
				throw new IllegalStateException();
			}
			
			JsonInput jsonInput = jsonCache.getJsonInput(inputFile);
			
			String contentAsString = jsonInput.getContentAsString(minimumLength);
			byte[] contentAsBytes = jsonInput.getContentAsBytes(minimumLength);
			
			String stringOutput = filter.process(contentAsString, metrics);
			byte[] byteOutput = filter.process(contentAsBytes, metrics);
			
			for(int i = 0; i < jsonInput.getPrettyPrintedSize(); i++) {
				String prettyPrintedAsString = jsonInput.getPrettyPrintedAsString(i, minimumLength);
				byte[] prettyPrintedAsBytes = jsonInput.getPrettyPrintedAsBytes(i, minimumLength);
				
				String prettyPrintStringOutput = filter.process(prettyPrintedAsString, metrics);
				byte[] prettyPrintBytesOutput = filter.process(prettyPrintedAsBytes, metrics);
				
				if(filter.isRemovingWhitespace()) {
					if(!Objects.equals(stringOutput, prettyPrintStringOutput)) {
						System.out.println(prettyPrintedAsString);
						System.out.println(prettyPrintStringOutput);
						System.out.println(stringOutput);
						fail("Expected symmertic pretty-printed string result for " + inputFile);
					}
					if(!Arrays.equals(byteOutput, prettyPrintBytesOutput)) {
						fail("Expected symmertic pretty-printed byte[] result for " + inputFile);
					}
				} else {
					if(!JsonComparator.isSameEvents(stringOutput, prettyPrintStringOutput)) {
						System.out.println(prettyPrintedAsString);
						System.out.println(stringOutput);
						System.out.println(prettyPrintStringOutput);
						fail("Expected event symmertic pretty-printed byte[] result for " + inputFile + " minimum length " + minimumLength + " -> " + prettyPrintedAsString.length());
					}
				}
				checkSymmetric(prettyPrintStringOutput, prettyPrintBytesOutput);
			}
			checkSymmetric(stringOutput, byteOutput);

			return new JsonFilterInputOutput(filter, new JsonInputOutput(contentAsString, stringOutput));
		}

		private boolean checkSymmetric(String outputAsString, byte[] outputAsBytes) {
			if(outputAsString == null && outputAsBytes == null) {
				return false;
			}
			if(outputAsString != null && outputAsBytes == null) {
				fail("Expected symmertic result for " + inputFile + ", but there was no byte output");
			}
			if(outputAsString == null && outputAsBytes != null) {
				fail("Expected symmertic result for " + inputFile + ", but there was no char output");
			}
			
			String outputAsBytesAsString = new String(outputAsBytes, StandardCharsets.UTF_8);
			if(outputAsBytesAsString.equals(outputAsString)) {
				return true;
			}
			
			// for unicode and max string size, bytes and chars are counted somewhat differently
			String outputAsBytesAsNormalizedString = JsonNormalizer.filterMaxStringLength(outputAsBytesAsString);
			String outputAsStringAsNormalizedString = JsonNormalizer.filterMaxStringLength(outputAsString);
			if(outputAsBytesAsNormalizedString.equals(outputAsStringAsNormalizedString)) {
				return true;
			}
			
			System.out.println(outputAsBytesAsString);
			System.out.println(outputAsString);
			fail("Expected symmertic result for " + inputFile + "\n" + outputAsBytesAsString + "\n" + outputAsString + "\n");

			return true;
		}

	}

	private final JsonFilter filter;
	private final JsonInputOutput jsonFilterResult;

	public JsonFilterInputOutput(JsonFilter filter, JsonInputOutput jsonFilterResult) {
		this.filter = filter;
		this.jsonFilterResult = jsonFilterResult;
	}

	public JsonFilter getFilter() {
		return filter;
	}

	public JsonInputOutput getResult() {
		return jsonFilterResult;
	}
	
}
