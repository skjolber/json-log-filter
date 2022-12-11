package com.github.skjolber.jsonfilter.test.truth;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.MaxSizeJsonFilterAdapter;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparator;
import com.github.skjolber.jsonfilter.test.jackson.JsonNormalizer;

public class JsonMaxSizeFilterInputOutput {

	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private MaxSizeJsonFilterAdapter adapter;
		private Path inputFile;
		
		// add padding so that the max length code paths are in use
		private int minimumLengthChars = -1;
		private int minimumLengthBytes = -1;
		private JsonCache jsonCache = JsonCache.getInstance();
		private JsonFilterMetrics metrics;
		private boolean whitespace = true;
		private boolean unicode = true;
		
		public Builder withMinimumLengthChars(int length) {
			this.minimumLengthChars = length;
			return this;
		}

		public Builder withWhitespace(boolean whitespace) {
			this.whitespace = whitespace;
			return this;
		}

		public Builder withMinimumLengthBytes(int length) {
			this.minimumLengthBytes = length;
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

		public Builder withFilter(MaxSizeJsonFilterAdapter adapter) {
			this.adapter = adapter;
			return this;
		}
		
		public Builder withUnicode(boolean unicode) {
			this.unicode = unicode;
			return this;
		}

		public JsonMaxSizeFilterInputOutput build() {
			if(inputFile == null) {
				throw new IllegalStateException();
			}
			if(adapter == null) {
				throw new IllegalStateException();
			}
			if(metrics == null) {
				throw new IllegalStateException();
			}
			
			JsonInput jsonInput = jsonCache.getJsonInput(inputFile);
			if(!unicode && (jsonInput.hasUnicode() || jsonInput.hasEscapeSequence())) {
				return null;
			}
			
			String contentAsString = jsonInput.getContentAsString(minimumLengthChars+6);
			byte[] contentAsBytes = jsonInput.getContentAsBytes(minimumLengthChars+6);
			
			JsonFilter charFilter = adapter.getMaxSize(minimumLengthChars + 5);
			JsonFilter byteFilter = adapter.getMaxSize(minimumLengthBytes + 5);
			
			String stringOutput = charFilter.process(contentAsString, metrics);
			byte[] byteOutput = byteFilter.process(contentAsBytes, metrics);
			
			checkSymmetric(stringOutput, byteOutput);

			if(whitespace) {
				for(int i = 0; i < jsonInput.getPrettyPrintedSize(); i++) {
					String prettyPrintedAsString = jsonInput.getPrettyPrintedAsString(i);
					byte[] prettyPrintedAsBytes = jsonInput.getPrettyPrintedAsBytes(i);
					
					JsonFilter prettyPrintCharFilter;
					JsonFilter prettyPrintByteFilter;
					if(charFilter.isRemovingWhitespace()) {
						prettyPrintCharFilter = charFilter;
						prettyPrintByteFilter = byteFilter;
					} else {
						// so the filter might quit prematurely because of whitespace 
						// also the replacements might be longer than the actual content, so account for that as well
						
						int expectedDifference = jsonInput.getContentAsStringSize() - minimumLengthChars;
						
						int stringLength = prettyPrintedAsString.length() - expectedDifference;
						int byteLength = prettyPrintedAsBytes.length - expectedDifference;
						
						prettyPrintCharFilter = adapter.getMaxSize(stringLength + 3);
						prettyPrintByteFilter = adapter.getMaxSize(byteLength + 3);
						
						prettyPrintedAsString = jsonInput.getPrettyPrintedAsString(i, stringLength + 4);
						prettyPrintedAsBytes = jsonInput.getPrettyPrintedAsBytes(i, byteLength + 4);
					}
					
					String prettyPrintStringOutput = prettyPrintCharFilter.process(prettyPrintedAsString, metrics);
					byte[] prettyPrintBytesOutput = prettyPrintByteFilter.process(prettyPrintedAsBytes, metrics);
					
					if(checkSymmetric(prettyPrintStringOutput, prettyPrintBytesOutput)) {
						if(charFilter.isRemovingWhitespace()) {
							if(!Objects.equals(stringOutput, prettyPrintStringOutput)) {
								System.out.println(inputFile);
								System.out.println(prettyPrintedAsString);
								System.out.println(stringOutput);
								System.out.println(prettyPrintStringOutput);
								fail("Expected symmertic pretty-printed string result for " + inputFile + " " + minimumLengthBytes);
							}
							if(!Arrays.equals(byteOutput, prettyPrintBytesOutput)) {
								System.out.println(inputFile);
								System.out.println(new String(byteOutput, StandardCharsets.UTF_8));
								System.out.println(new String(prettyPrintBytesOutput, StandardCharsets.UTF_8));
								fail("Expected symmertic pretty-printed byte[] result for " + inputFile);
							}
						} else {
							if(!JsonComparator.isSameEvents(stringOutput, prettyPrintStringOutput)) {
								System.out.println(prettyPrintedAsString);
								System.out.println(stringOutput);
								System.out.println(prettyPrintStringOutput);
								fail("Expected event symmertic pretty-printed byte[] result for " + inputFile + " minimum length " + minimumLengthChars + " -> " + prettyPrintedAsString.length());
							}
						}
					}
				}
			}

			return new JsonMaxSizeFilterInputOutput(charFilter, new JsonInputOutput(contentAsString, stringOutput));
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
			fail("Expected symmertic result for " + inputFile);

			return true;
		}

	}

	private final JsonFilter filter;
	private final JsonInputOutput jsonFilterResult;

	public JsonMaxSizeFilterInputOutput(JsonFilter filter, JsonInputOutput jsonFilterResult) {
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
