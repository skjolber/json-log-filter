package com.github.skjolber.jsonfilter.test;

import java.io.File;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.directory.JsonFilterDirectoryUnitTestFactory;
import com.github.skjolber.jsonfilter.test.directory.JsonFilterDirectoryUnitTest;
import com.github.skjolber.jsonfilter.test.directory.JsonFilterProperties;
import com.github.skjolber.jsonfilter.test.directory.JsonFilterPropertiesFactory;
import com.github.skjolber.jsonfilter.test.pp.PrettyPrintTransformer;
import com.github.skjolber.jsonfilter.test.truth.JsonFilterUnitTest;
import com.github.skjolber.jsonfilter.test.truth.JsonMaxSizeFilterUnitTest;

public class JsonFilterRunner {

	private JsonFileCache cache;
	private List<JsonFilterDirectoryUnitTest> outputDirectories;
	private boolean literal;
	
	private List<JsonFilterDirectoryUnitTest> directoryTests;

	public JsonFilterRunner(List<?> nullable, boolean literal, JsonFileCache cache) throws Exception {
		JsonFilterDirectoryUnitTestFactory factory = JsonFilterDirectoryUnitTestFactory.fromResource("/json", nullable);

		this.literal = literal;
		this.cache = cache;
		
		this.directoryTests = factory.create();
	}

	public JsonFilterDirectoryUnitTestCollection process(JsonFilter filter) throws Exception {
		JsonFilterDirectoryUnitTestCollection result = new JsonFilterDirectoryUnitTestCollection(); 
		for(JsonFilterDirectoryUnitTest directoryTest : directoryTests) {
			if(directoryTest.isNoop()) {
				JsonFilterProperties properties = new JsonFilterProperties(filter, directoryTest.getProperties());
				for (Path path : directoryTest.getFiles().keySet()) {
					JsonFilterUnitTest test = JsonFilterUnitTest.newBuilder()
							.withFilter(filter)
							.withInputFile(path)
							.withOutputFile(path)
							.withOutputProperties(properties)
							.withLiteral(literal)
							.build();
				}
				result.addPassthrough(directoryTest);
			} else {
				JsonFilterProperties properties = new JsonFilterProperties(filter, directoryTest.getProperties());
				 for (Entry<Path, Path> entry : directoryTest.getFiles().entrySet()) {
					JsonFilterUnitTest test = JsonFilterUnitTest.newBuilder()
							.withFilter(filter)
							.withInputFile(entry.getKey())
							.withOutputFile(entry.getValue())
							.withOutputProperties(properties)
							.withLiteral(literal)
							.build();
				}
				result.addFiltered(directoryTest);
			}
		}

		return result;
	}

	public JsonFilterDirectoryUnitTestCollection process(MaxSizeJsonFilterAdapter maxSizeFunction, JsonFilter infiniteSize) throws Exception {
		
		JsonFilterDirectoryUnitTestCollection result = new JsonFilterDirectoryUnitTestCollection(); 
		for(JsonFilterDirectoryUnitTest directoryTest : directoryTests) {
			if(directoryTest.isNoop()) {
				JsonFilterProperties properties = new JsonFilterProperties(maxSizeFunction.getMaxSize(-1), directoryTest.getProperties());
				for (Path path : directoryTest.getFiles().keySet()) {
					JsonMaxSizeFilterUnitTest test = JsonMaxSizeFilterUnitTest.newBuilder()
							.withAdapter(maxSizeFunction)
							.withInputFile(path)
							.withOutputFile(path)
							.withOutputProperties(properties)
							.withLiteral(literal)
							.build();
				}
				result.addPassthrough(directoryTest);
			} else {
				JsonFilterProperties properties = new JsonFilterProperties(maxSizeFunction.getMaxSize(-1), directoryTest.getProperties());
				 for (Entry<Path, Path> entry : directoryTest.getFiles().entrySet()) {
					 JsonMaxSizeFilterUnitTest test = JsonMaxSizeFilterUnitTest.newBuilder()
							.withAdapter(maxSizeFunction)
							.withInputFile(entry.getKey())
							.withOutputFile(entry.getValue())
							.withOutputProperties(properties)
							.withLiteral(literal)
							.build();
				}
				result.addFiltered(directoryTest);
			}
		}

		return result;

		
	}

}
