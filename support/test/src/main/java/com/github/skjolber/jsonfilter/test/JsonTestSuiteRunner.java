package com.github.skjolber.jsonfilter.test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.test.cache.JsonFile;
import com.github.skjolber.jsonfilter.test.cache.JsonFileCache;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair.MaxSizeJsonFilterFunction;
import com.github.skjolber.jsonfilter.test.directory.JsonFilterDirectoryUnitTest;
import com.github.skjolber.jsonfilter.test.directory.JsonFilterDirectoryUnitTestFactory;
import com.github.skjolber.jsonfilter.test.truth.JsonFilterSubject;

/**
 * 
 * Run JsonTestSuite resources (from https://github.com/nst/JSONTestSuite/tree/master)
 * 
 */

public class JsonTestSuiteRunner {
	
	public static JsonTestSuiteRunner fromResource(String path) throws URISyntaxException, IOException {
		URI uri = JsonFilterDirectoryUnitTestFactory.class.getProtectionDomain().getCodeSource().getLocation().toURI();
		
		Path uriPath = Paths.get(uri);
		
		if(path.startsWith("/")) {
			path = path.substring(1);
		}
		Path target = uriPath.resolve(path);

		Map<Path, Path> files = new HashMap<>();
		Properties properties = new Properties();
		
		if(Files.isDirectory(target)) {
			for(Path file : Files.newDirectoryStream(target, (f) -> Files.isRegularFile(f) && f.getFileName().toString().endsWith(".json"))) {
				files.put(file, file);
			}
		}
		return new JsonTestSuiteRunner(new JsonFilterDirectoryUnitTest(uriPath, files, properties), JsonFileCache.getInstance());
	}
	
	private final JsonFilterDirectoryUnitTest files;
	private final JsonFileCache cache;
	
	public JsonTestSuiteRunner(JsonFilterDirectoryUnitTest files, JsonFileCache cache) {
		this.files = files;
		this.cache = cache;
	}
	
	public JsonFilterDirectoryUnitTestCollection processDirectoryUnitTest(JsonFilter infiniteSize) throws Exception {
		
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();

		JsonFilterDirectoryUnitTestCollection result = new JsonFilterDirectoryUnitTestCollection(); 
		for (Path path : files.getFiles().keySet()) {
			JsonFile jsonInput = cache.getJsonInput(path, false);
			
			// remove pretty-printings
			JsonFile f = new JsonFile(jsonInput.getSource(), jsonInput.getContentAsString(), Collections.emptyList(), Collections.emptyList());
			
			JsonFilterSubject.assertThat(infiniteSize)
				.withInputFile(f)
				.withMetrics(metrics)
				.isPassthrough();
		}
		
		result.addPassthrough(files);
		
		return result;
	}
	
	public JsonFilterDirectoryUnitTestCollection processDirectoryUnitTest(MaxSizeJsonFilterFunction maxSizeFunction, JsonFilter infiniteSize) throws Exception {
		
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();

		JsonFilterDirectoryUnitTestCollection result = new JsonFilterDirectoryUnitTestCollection(); 
		for (Path path : files.getFiles().keySet()) {
			JsonFile jsonInput = cache.getJsonInput(path, false);
			
			// remove pretty-printings
			
			JsonFile f = new JsonFile(jsonInput.getSource(), jsonInput.getContentAsString(), Collections.emptyList(), Collections.emptyList());
			
			String contentAsString = jsonInput.getContentAsString();
			byte[] contentAsBytes = jsonInput.getContentAsBytes();
			
			JsonFilterSubject.assertThat(maxSizeFunction.getMaxSize(contentAsString.length()), maxSizeFunction.getMaxSize(contentAsBytes.length))
				.withInputFile(f)
				.withMetrics(metrics)
				.isPassthrough();
		}
		
		result.addPassthrough(files);
		
		return result;
	}
}
