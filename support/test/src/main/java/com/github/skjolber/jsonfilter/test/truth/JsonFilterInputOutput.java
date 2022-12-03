package com.github.skjolber.jsonfilter.test.truth;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparator;

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
		
		public Builder withMinimumLength(int length) {
			this.minimumLength = length;
			return this;
		}
		
		public Builder withInputFile(Path file) {
			this.inputFile = file;
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
			
			JsonInput jsonInput = jsonCache.getJsonInput(inputFile);
			
			String contentAsString = jsonInput.getContentAsString(minimumLength);
			byte[] contentAsBytes = jsonInput.getContentAsBytes(minimumLength);
			
			String stringOutput = filter.process(contentAsString);
			byte[] byteOutput = filter.process(contentAsBytes);
			
			System.out.println(contentAsString);
			System.out.println(stringOutput);
			
			checkSymmetric(stringOutput, byteOutput);

			for(int i = 0; i < jsonInput.getPrettyPrintedSize(); i++) {
				String prettyPrintedAsString = jsonInput.getPrettyPrintedAsString(i, minimumLength);
				byte[] prettyPrintedAsBytes = jsonInput.getPrettyPrintedAsBytes(i, minimumLength);
				
				String prettyPrintStringOutput = filter.process(prettyPrintedAsString);
				byte[] prettyPrintBytesOutput = filter.process(prettyPrintedAsBytes);
				
				if(checkSymmetric(prettyPrintStringOutput, prettyPrintBytesOutput)) {
					if(filter.isRemovingWhitespace()) {
						if(!Objects.equals(stringOutput, prettyPrintStringOutput)) {
							fail("Expected symmertic pretty-printed string result for " + inputFile);
						}
						if(!Arrays.equals(byteOutput, prettyPrintBytesOutput)) {
							fail("Expected symmertic pretty-printed byte[] result for " + inputFile);
						}
					} else {
						if(!JsonComparator.isSameEvents(stringOutput, prettyPrintStringOutput)) {
							fail("Expected event symmertic pretty-printed byte[] result for " + inputFile);
						}
					}
				}
			}

			return new JsonFilterInputOutput(filter, new JsonInputOutput(contentAsString, stringOutput));
		}

		private boolean checkSymmetric(String outputAsString, byte[] outputAsBytes) {
			if(outputAsString == null && outputAsBytes == null) {
				return false;
			}
			if(outputAsString != null && outputAsBytes == null) {
				fail("Expected symmertic result for " + inputFile);
			}
			if(outputAsString == null && outputAsBytes != null) {
				fail("Expected symmertic result for " + inputFile);
			}
			
			String outputAsBytesAsString = new String(outputAsBytes, StandardCharsets.UTF_8);
			if(!outputAsBytesAsString.equals(outputAsString)) {
				fail("Expected symmertic result for " + inputFile);
			}
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
