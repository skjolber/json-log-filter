package com.github.skjolber.jsonfilter.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

public class JsonFilterOutputDirectoriesFactory extends AbstractJsonFilterPropertiesFactory {

	public JsonFilterOutputDirectoriesFactory(List<?> nullable) {
		super(nullable);
	}

	public List<JsonFilterOutputDirectory> create(File directory) throws Exception {
		List<JsonFilterOutputDirectory> result = new ArrayList<>();

		List<File> parents = new ArrayList<>();
		
		processFilterDirectories(directory, result, parents, new ArrayList<>());
		
		return result;
	}
	
	protected void processFilterDirectories(File directory, List<JsonFilterOutputDirectory> results, List<File> parents, List<File> filterDirectoryParents) throws Exception {
		if(JsonFilterPropertiesFactory.isFilterDirectory(directory)) {
			filterDirectoryParents.add(directory);
			
			Properties sourceProperties = new Properties();
			
			for(File file : filterDirectoryParents) {
				Properties additional = readDirectoryProperties(file);
				sourceProperties.putAll(additional);
			}
			
			sourceProperties.putAll(readDirectoryProperties(directory));
			
			JsonFilterInputDirectory jsonFilterInputDirectory = new JsonFilterInputDirectory(new ArrayList<>(parents), sourceProperties);
			
			results.add(new JsonFilterOutputDirectory(directory, filterDirectoryParents, jsonFilterInputDirectory));
		} else {
			parents.add(directory);
		}

		File[] subdirectories = directory.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return new File(dir, name).isDirectory();
			}
		});

		if(subdirectories != null) {
			Arrays.sort(subdirectories);
			for(File subdirectory : subdirectories) {
				processFilterDirectories(subdirectory, results, new ArrayList<>(parents), new ArrayList<>(filterDirectoryParents));
			}
		}
	}

	private Properties normalize(Properties properties) {
		
		Properties normalizedProperties = new Properties();
		for (Entry<Object, Object> entry : properties.entrySet()) {
			put(normalizedProperties, entry.getValue(), (String)entry.getKey());
		}
		
		return normalizedProperties;
	}
	
	public Properties readDirectoryProperties(File directory) throws IOException {
		return normalize(readPropertiesFile(new File(directory, "filter.properties")));
	}
	
	public Properties readPropertiesFile(File filter) throws IOException {
		Properties properties = new Properties();
		FileInputStream fin = new FileInputStream(filter);
		try {
			properties.load(fin);
		} finally {
			fin.close();
		}
		return properties;
	}

}
