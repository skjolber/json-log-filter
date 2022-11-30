package com.github.skjolber.jsonfilter.test.directory;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class JsonFilterDirectoryUnitTest {

	protected final Properties properties;
	protected final Map<Path, Path> files;
	protected final Path location;
	
	public JsonFilterDirectoryUnitTest(Path location, Map<Path, Path> files, Properties properties) {
		this.location = location;
		this.files = files;
		this.properties = properties;
	}
	
	public boolean isNoop() {
		return properties == null || properties.isEmpty();
	}

	public Map<Path, Path> getFiles() {
		return files;
	}
	
	public Properties getProperties() {
		return properties;
	}

	public Path getLocation() {
		return location;
	}

	@Override
	public int hashCode() {
		return Objects.hash(files, location, properties);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JsonFilterDirectoryUnitTest other = (JsonFilterDirectoryUnitTest) obj;
		return Objects.equals(files, other.files) && Objects.equals(location, other.location)
				&& Objects.equals(properties, other.properties);
	}
	
	
}
