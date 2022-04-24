package com.github.skjolber.jsonfilter.test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class JsonFilterOutputDirectory {

	protected final JsonFilterInputDirectory input;
	protected final List<File> outputs;
	protected final File directory;
	
	public JsonFilterOutputDirectory(File directory, List<File> outputs, JsonFilterInputDirectory input) {
		this.outputs = outputs;
		this.input = input;
		this.directory = directory;
	}

	public JsonFilterInputDirectory getInputDirectories() {
		return input;
	}
	
	public Map<String, File> getFiles() {
		Map<String, File> map = new HashMap<>();
		for(File directory : outputs) {
			File[] files = JsonFilterInputDirectory.getFiles(directory);
			if(files != null) {
				for(File file : files) {
					map.put(file.getName(), file);
				}
			}

		}
		return map;
	}
	
	public Properties getProperties() {
		return input.getProperties();
	}
	
	public File getDirectory() {
		return directory;
	}

	@Override
	public int hashCode() {
		return Objects.hash(directory, input, outputs);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JsonFilterOutputDirectory other = (JsonFilterOutputDirectory) obj;
		return Objects.equals(directory, other.directory) && Objects.equals(input, other.input)
				&& Objects.equals(outputs, other.outputs);
	}
	
	
	
}
