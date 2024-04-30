package com.github.skjolber.jsonfilter.test.directory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class JsonFilterDirectoryUnitTestFactory {

	public static JsonFilterDirectoryUnitTestFactory fromResource(String path, List<?> nullable) throws URISyntaxException, IOException {
		URI uri = JsonFilterDirectoryUnitTestFactory.class.getProtectionDomain().getCodeSource().getLocation().toURI();
		
		Path uriPath = Paths.get(uri);

		if(Files.isDirectory(uriPath)) {
			if(path.startsWith("/")) {
				path = path.substring(1);
			}
			return new JsonFilterDirectoryUnitTestFactory(nullable, uriPath.resolve(path));
		} else {
			FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
			
			List<String> resourceNames;
			try (ScanResult scanResult = new ClassGraph().acceptPaths(path).scan()) {
			    resourceNames = scanResult.getAllResources().getPaths();
			}
			for(String str : resourceNames) {
				String s = "/" + str;

				Path p = fs.getPath(s);

				Files.createDirectories(p.getParent());

				InputStream resourceAsStream = JsonFilterDirectoryUnitTestFactory.class.getResourceAsStream(s);
				if(resourceAsStream == null) {
					throw new IOException(s);
				}
				
				byte[] byteArray = IOUtils.toByteArray(resourceAsStream);
				Files.write(p, byteArray);
			}
			return new JsonFilterDirectoryUnitTestFactory(nullable, fs.getPath(path));
		}		
	}

	public static class JsonDirectory {

		private Path directory;

		private Properties properties;

		private JsonDirectory parent;

		private Map<String, Path> jsonFiles;

		private boolean output = false;

		public Path getParentFile(String name) {
			Path file = null;
			JsonDirectory directory = this.parent;
			while(directory != null) {

				Path path = directory.jsonFiles.get(name);
				if(path != null) {
					file = path;
				}

				directory = directory.parent;
			}
			if(properties != null && !properties.isEmpty() && file == null) {
				throw new IllegalArgumentException("No file " + name + " any parent directory for " + this.directory + " for " + properties);
			}

			return file;
		}

	}

	private JsonFilterPropertiesFactory jsonFilterPropertiesFactory;
	private Path directory;

	public JsonFilterDirectoryUnitTestFactory(List<?> nullable, Path directory) {
		this.jsonFilterPropertiesFactory = new JsonFilterPropertiesFactory(nullable,  Arrays.asList("removingWhitespace", "validating"));
		this.directory = directory;
	}

	public List<JsonFilterDirectoryUnitTest> create() throws Exception {
		List<JsonFilterDirectoryUnitTest> result = new ArrayList<>();

		JsonDirectory jsonDirectory = createJsonDirectory(null, directory);

		processFilterDirectories(jsonDirectory, result);

		return result;
	}

	public JsonFilterProperties getProperties(JsonFilter filter) {
		return jsonFilterPropertiesFactory.createInstance(filter);
	}

	protected JsonDirectory createJsonDirectory(JsonDirectory parent, Path directory) throws IOException {
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

		DirectoryStream<Path> files = Files.newDirectoryStream(jsonDirectory.directory, (f) -> Files.isRegularFile(f) && f.getFileName().toString().endsWith(".json"));
		try {
			for(Path file : files) {
				jsonDirectory.jsonFiles.put(file.getFileName().toString(), file); 
			}
		} finally {
			files.close();
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

		DirectoryStream<Path> stream = Files.newDirectoryStream(jsonDirectory.directory, Files::isDirectory);
		try {
			for(Path subdirectory : stream) {
				JsonDirectory createJsonDirectory = createJsonDirectory(jsonDirectory, subdirectory);
	
				processFilterDirectories(createJsonDirectory, results);
			}
		} finally {
			stream.close();
		}
	}

	protected Map<Path, Path> getInputFiles(JsonDirectory jsonDirectory) {
		Map<Path, Path> map = new HashMap<>();
		for (Entry<String, Path> entry : jsonDirectory.jsonFiles.entrySet()) {
			String key = entry.getKey();

			Path from = jsonDirectory.getParentFile(key);
			if(from != null) {
				Path to = entry.getValue();

				map.put(from,  to);
			}
		}
		return map;
	}

}
