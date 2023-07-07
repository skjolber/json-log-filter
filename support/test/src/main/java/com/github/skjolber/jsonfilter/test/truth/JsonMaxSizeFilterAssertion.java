package com.github.skjolber.jsonfilter.test.truth;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.MaxSizeJsonFilterFunction;
import com.github.skjolber.jsonfilter.test.cache.JsonFileCache;
import com.github.skjolber.jsonfilter.test.directory.JsonFilterProperties;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparator;
import com.github.skjolber.jsonfilter.test.jackson.JsonNormalizer;

public class JsonMaxSizeFilterAssertion {

	public static JsonMaxSizeFilterAssertion newInstance() {
		return new JsonMaxSizeFilterAssertion();
	}
	
	private Path inputFile;
	private Path outputFile;
	private boolean literal = true;
	private MaxSizeJsonFilterFunction maxSizeJsonFilterFunction;
	private JsonFileCache cache;
	private JsonFilterMetrics metrics;
	private boolean whitespace;
	private boolean unicode = true;

	public JsonMaxSizeFilterAssertion withWhitespace(boolean whitespace) {
		this.whitespace = whitespace;
		return this;
	}
	
	public JsonMaxSizeFilterAssertion withMetrics(JsonFilterMetrics metrics) {
		this.metrics = metrics;
		return this;
	}
	
	public JsonMaxSizeFilterAssertion withMaxSizeJsonFilterFunction(MaxSizeJsonFilterFunction adapter) {
		this.maxSizeJsonFilterFunction = adapter;
		return this;
	}
	
	public JsonMaxSizeFilterAssertion withLiteral(boolean enabled) {
		this.literal = enabled;
		return this;
	}
	
	public JsonMaxSizeFilterAssertion withInputFile(Path file) {
		this.inputFile = file;
		return this;
	}

	public JsonMaxSizeFilterAssertion withOutputFile(Path file) {
		this.outputFile = file;
		return this;
	}

	public JsonMaxSizeFilterAssertion withUnicode(boolean unicode) {
		this.unicode  = unicode;
		return this;
	}
	
	public JsonMaxSizeFilterAssertion build() {
		if(inputFile == null) {
			throw new IllegalStateException();
		}
		if(outputFile == null) {
			throw new IllegalStateException();
		}
		if(maxSizeJsonFilterFunction == null) {
			throw new IllegalStateException();
		}
		if(cache == null) {
			cache = JsonFileCache.getInstance();
		}

		String expectedJsonOutput = cache.getFile(outputFile);
		
		MaxSizeJsonFilterSymmetryAssertion.newInstance()
				.withMaxSizeJsonFilterFunction(maxSizeJsonFilterFunction)
				.withInputFile(inputFile)
				.withMetrics(metrics)
				.withUnicode(unicode)
				.withMinimumLengthChars(expectedJsonOutput.length())
				.withMinimumLengthBytes(expectedJsonOutput.getBytes(StandardCharsets.UTF_8).length)
				.withWhitespace(whitespace)
				.isSymmetric();
		
		JsonFilterResult result = jsonFilterInputOutput.getResult();
		if(!result.hasStringOutput()) {
			throw new IllegalStateException();
		}

		if(!isEqual(expectedJsonOutput, result.getStringOutput())) {
			System.out.println("Unexpected result for " + inputFile);
			System.out.println("Input   :" + result.getStringInput());
			System.out.println("Output  :" + result.getStringOutput());
			System.out.println("Expected:" + expectedJsonOutput);
			throw new IllegalStateException("Unexpected result");
		}
		return new JsonMaxSizeFilterAssertion(result.getStringOutput(), jsonFilterInputOutput.getFilter(), jsonFilterInputOutput.getResult());
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
