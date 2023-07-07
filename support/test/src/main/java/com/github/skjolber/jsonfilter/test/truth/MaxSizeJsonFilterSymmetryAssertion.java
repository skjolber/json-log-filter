package com.github.skjolber.jsonfilter.test.truth;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.MaxSizeJsonFilterFunction;
import com.github.skjolber.jsonfilter.test.cache.JsonFile;
import com.github.skjolber.jsonfilter.test.cache.JsonFileCache;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparator;
import com.github.skjolber.jsonfilter.test.jackson.JsonNormalizer;

public class MaxSizeJsonFilterSymmetryAssertion {

	public static MaxSizeJsonFilterSymmetryAssertion newInstance() {
		return new MaxSizeJsonFilterSymmetryAssertion();
	}
	
	private Path inputFile;

	private boolean unicode = true;
	private boolean whitespace = true;

	private JsonFileCache jsonCache;
	private JsonFilterMetrics metrics;

	private MaxSizeJsonFilterFunction maxSizeJsonFilterFunction;
	// add padding so that the max length code paths are in use
	private int minimumLengthChars = -1;
	private int minimumLengthBytes = -1;

	public MaxSizeJsonFilterSymmetryAssertion withJsonCache(JsonFileCache jsonCache) {
		this.jsonCache = jsonCache;
		return this;
	}
	
	public MaxSizeJsonFilterSymmetryAssertion withMinimumLengthChars(int length) {
		this.minimumLengthChars = length;
		return this;
	}

	public MaxSizeJsonFilterSymmetryAssertion withWhitespace(boolean whitespace) {
		this.whitespace = whitespace;
		return this;
	}

	public MaxSizeJsonFilterSymmetryAssertion withMinimumLengthBytes(int length) {
		this.minimumLengthBytes = length;
		return this;
	}

	public MaxSizeJsonFilterSymmetryAssertion withInputFile(Path file) {
		this.inputFile = file;
		return this;
	}
	
	public MaxSizeJsonFilterSymmetryAssertion withMetrics(JsonFilterMetrics metrics) {
		this.metrics = metrics;
		return this;
	}

	public MaxSizeJsonFilterSymmetryAssertion withMaxSizeJsonFilterFunction(MaxSizeJsonFilterFunction adapter) {
		this.maxSizeJsonFilterFunction = adapter;
		return this;
	}
	
	public MaxSizeJsonFilterSymmetryAssertion withUnicode(boolean unicode) {
		this.unicode = unicode;
		return this;
	}

	public void isSymmetric() {
		if(inputFile == null) {
			throw new IllegalStateException();
		}
		if(maxSizeJsonFilterFunction == null) {
			throw new IllegalStateException();
		}
		if(metrics == null) {
			throw new IllegalStateException();
		}
		if(jsonCache == null) {
			 jsonCache = JsonFileCache.getInstance();
		}
		
		JsonFile jsonInput = jsonCache.getJsonInput(inputFile);
		if(!unicode && (jsonInput.hasUnicode() || jsonInput.hasEscapeSequence())) {
			return;
		}
		
		String contentAsString = jsonInput.getContentAsString(minimumLengthChars);
		byte[] contentAsBytes = jsonInput.getContentAsBytes(minimumLengthChars);
		
		JsonFilter charFilter = maxSizeJsonFilterFunction.getMaxSize(minimumLengthChars);
		JsonFilter byteFilter = maxSizeJsonFilterFunction.getMaxSize(minimumLengthBytes);
		
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
					
					prettyPrintCharFilter = maxSizeJsonFilterFunction.getMaxSize(stringLength);
					prettyPrintByteFilter = maxSizeJsonFilterFunction.getMaxSize(byteLength);
					
					prettyPrintedAsString = jsonInput.getPrettyPrintedAsString(i, stringLength);
					prettyPrintedAsBytes = jsonInput.getPrettyPrintedAsBytes(i, byteLength);
				}
				
				String prettyPrintStringOutput = prettyPrintCharFilter.process(prettyPrintedAsString, metrics);
				byte[] prettyPrintBytesOutput = prettyPrintByteFilter.process(prettyPrintedAsBytes, metrics);
				
				if(checkSymmetric(prettyPrintStringOutput, prettyPrintBytesOutput)) {
					if(charFilter.isRemovingWhitespace()) {
						if(!Objects.equals(stringOutput, prettyPrintStringOutput)) {
							System.out.println();
							System.out.println(inputFile);
							System.out.println(contentAsString);
							System.out.println(prettyPrintedAsString);
							System.out.println(charFilter.getClass().getName() + ": " + charFilter);
							System.out.println(stringOutput);
							System.out.println(prettyPrintCharFilter.getClass().getName() + ": " + prettyPrintCharFilter);
							System.out.println(prettyPrintStringOutput);
							fail("Expected symmertic pretty-printed string result for " + inputFile + " " + minimumLengthBytes);
						}
						if(!Arrays.equals(byteOutput, prettyPrintBytesOutput)) {
							System.out.println();
							System.out.println(inputFile);
							System.out.println(new String(byteOutput, StandardCharsets.UTF_8));
							System.out.println(new String(prettyPrintBytesOutput, StandardCharsets.UTF_8));
							fail("Expected symmertic pretty-printed byte[] result for " + inputFile);
						}
					} else {
						if(!JsonComparator.isSameEvents(stringOutput, prettyPrintStringOutput)) {
							System.out.println();
							System.out.println(prettyPrintedAsString);
							System.out.println(stringOutput);
							System.out.println(prettyPrintStringOutput);
							fail("Expected event symmertic pretty-printed byte[] result for " + inputFile + " minimum length " + minimumLengthChars + " -> " + prettyPrintedAsString.length());
						}
					}
				}
			}
		}
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
		
		System.out.println(inputFile);
		System.out.println("Bytes:");
		System.out.println(outputAsBytesAsString);
		System.out.println("Chars:");
		System.out.println(outputAsString);
		fail("Expected symmertic result for " + inputFile);

		return true;
	}


	
}
