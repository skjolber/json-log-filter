package com.github.skjolber.jsonfilter.test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;

public class JsonFileCache {

	private static final JsonFileCache instance = new JsonFileCache();
	
	public static JsonFileCache getInstance() {
		return instance;
	}
	
	protected Map<Path, String> cache = new ConcurrentHashMap<>();

	public String getFile(Path path) {
		try {
			String string = cache.get(path);
			if(string == null) {
				try (InputStream in = Files.newInputStream(path)) {
					string = IOUtils.toString(in, "UTF-8");
					cache.put(path, string);
				}
			}
			return string;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void clear() {
		cache.clear();
	}
}
