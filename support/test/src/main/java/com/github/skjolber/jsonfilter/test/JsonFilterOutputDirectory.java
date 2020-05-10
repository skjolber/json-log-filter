package com.github.skjolber.jsonfilter.test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class JsonFilterOutputDirectory {

	protected final JsonFilterInputDirectory input;
	protected final List<File> outputs;
	
	public JsonFilterOutputDirectory(List<File> outputs, JsonFilterInputDirectory input) {
		this.outputs = outputs;
		this.input = input;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((input == null) ? 0 : input.hashCode());
		result = prime * result + ((outputs == null) ? 0 : outputs.hashCode());
		return result;
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
		if (input == null) {
			if (other.input != null)
				return false;
		} else if (!input.equals(other.input))
			return false;
		if (outputs == null) {
			if (other.outputs != null)
				return false;
		} else if (!outputs.equals(other.outputs))
			return false;
		return true;
	}


	
	
}
