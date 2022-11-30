package com.github.skjolber.jsonfilter.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.test.directory.JsonFilterDirectoryUnitTest;
import com.github.skjolber.jsonfilter.test.directory.JsonFilterDirectoryUnitTestFactory;

public class JsonFilterDirectoryUnitTestFactoryTest {

	@Test
	public void testReadDirectories() throws Exception {
		Path file = Path.of("./src/main/resources");
		
		JsonFilterDirectoryUnitTestFactory factory = new JsonFilterDirectoryUnitTestFactory(Collections.emptyList());
		
		List<JsonFilterDirectoryUnitTest> results = factory.create(file);
		
		assertTrue(results.size() > 80);
		
		for(JsonFilterDirectoryUnitTest result : results) {
			Map<Path, Path> files = result.getFiles();
			
			System.out.println(result.getLocation());
			
			for (Entry<Path, Path> entry : files.entrySet()) {
				System.out.println(" " + entry.getKey() + " -> " + entry.getValue());
			}
		}
	}
}
