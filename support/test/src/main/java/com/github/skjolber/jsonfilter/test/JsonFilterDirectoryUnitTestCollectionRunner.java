package com.github.skjolber.jsonfilter.test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map.Entry;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.test.cache.JsonFile;
import com.github.skjolber.jsonfilter.test.cache.JsonFileCache;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair.MaxSizeJsonFilterFunction;
import com.github.skjolber.jsonfilter.test.directory.JsonFilterDirectoryUnitTest;
import com.github.skjolber.jsonfilter.test.directory.JsonFilterDirectoryUnitTestFactory;
import com.github.skjolber.jsonfilter.test.directory.JsonFilterProperties;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparisonType;
import com.github.skjolber.jsonfilter.test.truth.JsonFilterSubject;

public class JsonFilterDirectoryUnitTestCollectionRunner {

	private JsonFileCache cache;
	private List<JsonFilterDirectoryUnitTest> outputDirectories;
	private boolean literal;
	// test json with unicode
	private boolean unicode;

	private List<JsonFilterDirectoryUnitTest> directoryTests;
	private JsonFilterDirectoryUnitTestFactory factory;
	
	public JsonFilterDirectoryUnitTestCollectionRunner(List<?> nullable, boolean literal, boolean unicode, JsonFileCache cache) throws Exception {
		this.factory = JsonFilterDirectoryUnitTestFactory.fromResource("/json", nullable);
		this.literal = literal;
		this.cache = cache;
		this.unicode = unicode;
		this.directoryTests = factory.create();
	}

	public JsonFilterDirectoryUnitTestCollection processDirectoryUnitTest(JsonFilter filter) throws Exception {
		JsonFilterDirectoryUnitTestCollection result = new JsonFilterDirectoryUnitTestCollection(); 

		JsonFilterProperties filterProperties = factory.getProperties(filter);

		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();

		for(JsonFilterDirectoryUnitTest directoryTest : directoryTests) {
			if(filterProperties.isNoop()) {
				for (Path path : directoryTest.getFiles().keySet()) {
					
					JsonFile jsonInput = cache.getJsonInput(path);
					
					if(!unicode && (jsonInput.hasUnicode() || jsonInput.hasEscapeSequence())) {
						continue;
					}
					
					JsonFilterSubject.assertThat(filter).withMetrics(metrics).withInputFile(jsonInput).isPassthrough();
				}
				result.addPassthrough(directoryTest);
			} else if (filterProperties.matches(directoryTest.getProperties()) ){
				for (Entry<Path, Path> entry : directoryTest.getFiles().entrySet()) {
					System.out.println(entry.getKey() + " " + entry.getValue());
					
					JsonFile jsonInput = cache.getJsonInput(entry.getKey());
					
					if(!unicode && (jsonInput.hasUnicode() || jsonInput.hasEscapeSequence())) {
						continue;
					}

					JsonFile jsonOutput = cache.getJsonInput(entry.getValue());
					
					JsonFilterSubject.assertThat(filter).withMetrics(metrics).withInputFile(jsonInput).filtersTo(jsonOutput, literal ? JsonComparisonType.LITERAL : JsonComparisonType.EVENTS);
				}
				result.addFiltered(directoryTest);
			}
		}
		
		result.add(metrics);

		return result;
	}

	public JsonFilterDirectoryUnitTestCollection processDirectoryUnitTest(MaxSizeJsonFilterFunction maxSizeFunction, JsonFilter infiniteSize) throws Exception {

		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();

		JsonFilterProperties filterProperties = factory.getProperties(infiniteSize);

		JsonFilterDirectoryUnitTestCollection result = new JsonFilterDirectoryUnitTestCollection(); 
		for(JsonFilterDirectoryUnitTest directoryTest : directoryTests) {
			if(filterProperties.isNoop()) {
				for (Path path : directoryTest.getFiles().keySet()) {
					JsonFile jsonInput = cache.getJsonInput(path);
					
					if(!unicode && (jsonInput.hasUnicode() || jsonInput.hasEscapeSequence())) {
						continue;
					}
					
					JsonFilterSubject.assertThat(infiniteSize)
						.withInputFile(jsonInput)
						.withMaxSizeJsonFilterFunction(maxSizeFunction)
						.withMetrics(metrics)
						.withInputFile(jsonInput)
						.isPassthrough();
				}
				result.addPassthrough(directoryTest);
			} else if (filterProperties.matches(directoryTest.getProperties()) ){
				for (Entry<Path, Path> entry : directoryTest.getFiles().entrySet()) {
					
					JsonFile jsonInput = cache.getJsonInput(entry.getKey());
					
					if(!unicode && (jsonInput.hasUnicode() || jsonInput.hasEscapeSequence())) {
						continue;
					}

					JsonFile jsonOutput = cache.getJsonInput(entry.getValue());
					
					JsonFilterSubject.assertThat(infiniteSize)
						.withMaxSizeJsonFilterFunction(maxSizeFunction)
						.withMetrics(metrics)
						.withInputFile(jsonInput)
						.filtersTo(jsonOutput, literal ? JsonComparisonType.LITERAL : JsonComparisonType.EVENTS);

				}
				result.addFiltered(directoryTest);
			}
		}
		
		result.add(metrics);

		return result;
	}

}
