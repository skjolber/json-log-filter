package com.github.skjolber.jsonfilter.test;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;

public class JsonFileCache {

	private static final JsonFileCache instance = new JsonFileCache();
	
	public static JsonFileCache getInstance() {
		return instance;
	}
	
	protected Map<File, String> cache = new ConcurrentHashMap<>();

	public String getFile(File file) {
		try {
			String string = cache.get(file);
			if(string == null) {
				string = IOUtils.toString(file.toURI().toURL(), "UTF-8");
				cache.put(file, string);
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
