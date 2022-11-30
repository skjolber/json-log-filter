package com.github.skjolber.jsonfilter.test.directory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class JsonFilterDirectoryUnitTestFactory {

	public static class JsonDirectory {

		private Path directory;
		
		private Properties properties;
		
		private JsonDirectory parent;
		
		private Map<String, Path> jsonFiles;
		
		private boolean output = false;

	}

	private JsonFilterPropertiesFactory jsonFilterPropertiesFactory;
	
	public JsonFilterDirectoryUnitTestFactory(List<?> nullable) {
		jsonFilterPropertiesFactory = new JsonFilterPropertiesFactory(nullable);
	}

	public List<JsonFilterDirectoryUnitTest> create(Path directory) throws Exception {
		List<JsonFilterDirectoryUnitTest> result = new ArrayList<>();

		JsonDirectory jsonDirectory = createJsonDirectory(null, directory);
		
		processFilterDirectories(jsonDirectory, result);
		
		return result;
	}

	protected JsonDirectory createJsonDirectory(JsonDirectory parent, Path directory) throws IOException {
		System.out.println("Scan " + directory);
		JsonDirectory jsonDirectory = new JsonDirectory();
		jsonDirectory.directory = directory;
		
		Properties properties = new Properties();
		
		Properties parentProperties = getParentProperties(parent);
		if(parentProperties != null) {
			properties.putAll(parentProperties);
		}
		
		jsonDirectory.output = JsonFilterPropertiesFactory.isFilterDirectory(directory);
		
		if(jsonDirectory.output) {
			properties.putAll(jsonFilterPropertiesFactory.readDirectoryProperties(directory));
		}
		
		jsonDirectory.properties = properties;
		
		jsonDirectory.jsonFiles = new HashMap<>();
		
		if(parent != null) {
			jsonDirectory.jsonFiles.putAll(parent.jsonFiles);
		}

		for(Path file : Files.newDirectoryStream(jsonDirectory.directory, (f) -> Files.isRegularFile(f) && f.getFileName().toString().endsWith(".json"))) {
			jsonDirectory.jsonFiles.put(file.getFileName().toString(), file); 
		}
		
		jsonDirectory.parent = parent;
		
		return jsonDirectory;
	}
	
	private Properties getParentProperties(JsonDirectory parent) {
		while(parent != null) {
			if(parent.properties != null) {
				return parent.properties;
			}
			
			parent = parent.parent;
		}

		return null;
	}
	
	protected void processFilterDirectories(JsonDirectory jsonDirectory, List<JsonFilterDirectoryUnitTest> results) throws Exception {
		
		if(jsonDirectory.output) {
			results.add(new JsonFilterDirectoryUnitTest(jsonDirectory.directory, getInputFiles(jsonDirectory), jsonDirectory.properties));
		}

		for(Path subdirectory : Files.newDirectoryStream(jsonDirectory.directory, Files::isDirectory)) {
			JsonDirectory createJsonDirectory = createJsonDirectory(jsonDirectory, subdirectory);
			
			processFilterDirectories(createJsonDirectory, results);
		}
	}

	protected Map<Path, Path> getInputFiles(JsonDirectory jsonDirectory) {
		JsonDirectory parent = jsonDirectory.parent;
		
		Map<Path, Path> map = new HashMap<>();
		for (Entry<String, Path> entry : jsonDirectory.jsonFiles.entrySet()) {
			String key = entry.getKey();
			Path from = parent.jsonFiles.get(key);
			if(from == null) {
				throw new IllegalArgumentException("No file " + key + " in " + parent.directory + " for " + jsonDirectory.directory);
			}
			Path to = entry.getValue();

			map.put(from,  to);
		}
		return map;
	}

}
