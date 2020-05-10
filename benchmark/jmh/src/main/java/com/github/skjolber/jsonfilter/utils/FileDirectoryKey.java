package com.github.skjolber.jsonfilter.utils;

import java.io.FileFilter;

public class FileDirectoryKey {

	private String resource;
	private FileFilter filter;
	
	public FileDirectoryKey(String resource, FileFilter filter) {
		this.resource = resource;
		this.filter = filter;
	}

	@Override
	public int hashCode() {
		return resource.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileDirectoryKey other = (FileDirectoryKey) obj;
		
		return other.resource.equals(resource) && other.filter == filter;
	}

	
	
}
