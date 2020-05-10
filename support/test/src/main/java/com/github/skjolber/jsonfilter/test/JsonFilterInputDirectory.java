package com.github.skjolber.jsonfilter.test;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Virtual input directory; a list of input directories above it.
 * 
 */

public class JsonFilterInputDirectory {

	public static File[] getFiles(File directory) {
		File[] files = directory.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".json");
			}
		});
		
		if(files == null || files.length == 0) {
			return null;
		}
		Arrays.sort(files);
		
		return files;

	}
	
	protected final Properties properties;
	// in order; parent first then the child, so that the child file overrides the parent
	protected final List<File> directories; 
	
	public JsonFilterInputDirectory(List<File> directories, Properties properties) {
		for(File directory : directories) {
			if(directory == null) {
				throw new IllegalArgumentException();
			}
		}

		this.directories = directories;
		this.properties = properties;
	}

	public boolean matches(JsonFilterProperties wrapper) {
		if(wrapper.getProperties().equals(properties)) {
			return true;
		}
		
		return false;
	}
	
	public Map<String, File> getFiles() {
		Map<String, File> map = new HashMap<>();
		for(File directory : directories) {
			File[] files = getFiles(directory);
			if(files != null) {
				for(File file : files) {
					map.put(file.getName(), file);
				}
			}

		}
		return map;
	}

	public boolean hasProperties() {
		return properties != null;
	}

	public Properties getProperties() {
		return properties;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((directories == null) ? 0 : directories.hashCode());
		result = prime * result + ((properties == null) ? 0 : properties.hashCode());
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
		JsonFilterInputDirectory other = (JsonFilterInputDirectory) obj;
		if (directories == null) {
			if (other.directories != null)
				return false;
		} else if (!directories.equals(other.directories))
			return false;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		return true;
	}

	
}
