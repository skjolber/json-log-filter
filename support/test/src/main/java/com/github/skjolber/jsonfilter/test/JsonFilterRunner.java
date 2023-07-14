package com.github.skjolber.jsonfilter.test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map.Entry;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.test.cache.JsonFile;
import com.github.skjolber.jsonfilter.test.cache.JsonFileCache;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterFunction;
import com.github.skjolber.jsonfilter.test.directory.JsonFilterDirectoryUnitTest;
import com.github.skjolber.jsonfilter.test.directory.JsonFilterDirectoryUnitTestFactory;
import com.github.skjolber.jsonfilter.test.directory.JsonFilterProperties;
import com.github.skjolber.jsonfilter.test.truth.JsonFilterUnitTest;
import com.github.skjolber.jsonfilter.test.truth.MaxSizeJsonFilterAssertion;

public class JsonFilterRunner {

	private JsonFileCache cache;
	private List<JsonFilterDirectoryUnitTest> outputDirectories;
	private boolean literal;
	// test filters with pretty printed JSON
	private boolean whitespace;
	// test json with unicode
	private boolean unicode;

	private List<JsonFilterDirectoryUnitTest> directoryTests;
	private JsonFilterDirectoryUnitTestFactory factory;
	

	public JsonFilterRunner(List<?> nullable, boolean literal, boolean whitespace, boolean unicode, JsonFileCache cache) throws Exception {
		this.factory = JsonFilterDirectoryUnitTestFactory.fromResource("/json", nullable);
		this.literal = literal;
		this.whitespace = whitespace;
		this.cache = cache;
		this.unicode = unicode;
		this.directoryTests = factory.create();
	}

	public JsonFilterDirectoryUnitTestCollection process(JsonFilter filter) throws Exception {
		JsonFilterDirectoryUnitTestCollection result = new JsonFilterDirectoryUnitTestCollection(); 

		JsonFilterProperties filterProperties = factory.getProperties(filter);

		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();

		for(JsonFilterDirectoryUnitTest directoryTest : directoryTests) {
			if(filterProperties.isNoop()) {
				JsonFilterProperties properties = new JsonFilterProperties(filter, directoryTest.getProperties());
				for (Path path : directoryTest.getFiles().keySet()) {
					
					JsonFile jsonInput = cache.getJsonInput(path);
					
					if(!unicode && (jsonInput.hasUnicode() || jsonInput.hasEscapeSequence())) {
						continue;
					}
					
					JsonFilterUnitTest test = JsonFilterUnitTest.newBuilder()
							.withFilter(filter)
							.withInputFile(path)
							.withOutputFile(path)
							.withUnicode(unicode)
							.withFilterProperties(properties)
							.withLiteral(literal)
							.withMetrics(metrics)
							.withWhitespace(whitespace)
							.build();
				}
				result.addPassthrough(directoryTest);
			} else if (filterProperties.matches(directoryTest.getProperties()) ){
				JsonFilterProperties properties = new JsonFilterProperties(filter, directoryTest.getProperties());
				for (Entry<Path, Path> entry : directoryTest.getFiles().entrySet()) {
					JsonFilterUnitTest test = JsonFilterUnitTest.newBuilder()
							.withFilter(filter)
							.withInputFile(entry.getKey())
							.withOutputFile(entry.getValue())
							.withFilterProperties(properties)
							.withLiteral(literal)
							.withUnicode(unicode)
							.withWhitespace(whitespace)
							.withMetrics(metrics)
							.build();
				}
				result.addFiltered(directoryTest);
			}
		}
		
		result.add(metrics);

		return result;
	}

	public JsonFilterDirectoryUnitTestCollection process(MaxSizeJsonFilterFunction maxSizeFunction, JsonFilter infiniteSize) throws Exception {

		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();

		JsonFilterProperties filterProperties = factory.getProperties(infiniteSize);

		JsonFilterDirectoryUnitTestCollection result = new JsonFilterDirectoryUnitTestCollection(); 
		for(JsonFilterDirectoryUnitTest directoryTest : directoryTests) {
			if(filterProperties.isNoop()) {
				JsonFilterProperties properties = new JsonFilterProperties(maxSizeFunction.getMaxSizeJsonFilter(-1), directoryTest.getProperties());
				for (Path path : directoryTest.getFiles().keySet()) {
					MaxSizeJsonFilterAssertion test = MaxSizeJsonFilterAssertion.newBuilder()
							.withMaxSizeJsonFilterFunction(maxSizeFunction)
							.withInputFile(path)
							.withOutputFile(path)
							.withFilterProperties(properties)
							.withWhitespace(whitespace)
							.withUnicode(unicode)
							.withLiteral(literal)
							.withMetrics(metrics)
							.build();
				}
				result.addPassthrough(directoryTest);
			} else if (filterProperties.matches(directoryTest.getProperties()) ){
				JsonFilterProperties properties = new JsonFilterProperties(maxSizeFunction.getMaxSizeJsonFilter(-1), directoryTest.getProperties());
				for (Entry<Path, Path> entry : directoryTest.getFiles().entrySet()) {
					MaxSizeJsonFilterAssertion test = MaxSizeJsonFilterAssertion.newBuilder()
							.withMaxSizeJsonFilterFunction(maxSizeFunction)
							.withInputFile(entry.getKey())
							.withOutputFile(entry.getValue())
							.withFilterProperties(properties)
							.withWhitespace(whitespace)
							.withUnicode(unicode)
							.withLiteral(literal)
							.withMetrics(metrics)
							.build();
				}
				result.addFiltered(directoryTest);
			}
		}
		
		result.add(metrics);

		return result;
	}

}
