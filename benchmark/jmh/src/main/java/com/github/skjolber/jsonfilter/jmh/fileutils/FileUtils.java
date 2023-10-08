package com.github.skjolber.jsonfilter.jmh.fileutils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * For testing only
 * 
 * @author thomas
 *
 */

public class FileUtils {

	private static FileDirectoryCache directoryCache = new FileDirectoryCache();
	
	private static Map<File, String> fileCache = new ConcurrentHashMap<>();

	private static Map<String, URL> urlCache = new ConcurrentHashMap<>();

	private static Map<String, File> resourceCache = new ConcurrentHashMap<>();

	public static String read(File file) throws IOException {
		
		if(fileCache.containsKey(file)) {
			return fileCache.get(file);
		}
		
		if(!file.exists()) {
			throw new IllegalArgumentException(file.toString());
		}
		
		long size = file.length();
		
		byte[] buffer = new byte[(int) size];
		FileInputStream fin = new FileInputStream(file);
		DataInputStream din = new DataInputStream(fin);
		try {
			din.readFully(buffer);
		} finally {
			din.close();
		}
		
		String value = new String(buffer, StandardCharsets.UTF_8);
		
		fileCache.put(file, value);
		
		return value;
	}
	
	public static final String read(String resource) throws Exception {
		File file = new File(resource);
		return read(file);
		
	}
	
	public static URL getResourceAsURL(String resource) {
		if(urlCache.containsKey(resource)) {
			return urlCache.get(resource);
		}
		
		ClassLoader cl = FileUtils.class.getClassLoader();
		URL url = cl.getResource(resource);
		
		urlCache.put(resource, url);
		
		return url;
	}
	
	public static File getResourcesAsFile(String resource) {
		if(resourceCache.containsKey(resource)) {
			return resourceCache.get(resource);
		}
		
		URL inputURL = getResourceAsURL(resource);
		if(inputURL == null) {
			throw new RuntimeException(resource);
		}
		File file = new File(inputURL.getFile());

		resourceCache.put(resource, file);

		return file;
	}

	public static FileDirectoryValue getValue(String input, final FileFilter filter) throws FileNotFoundException, IOException {
		return getValue(input, filter, Function.identity());
	}
	
	public static FileDirectoryValue getValue(String input, final FileFilter filter, Function<String, String> transformer) throws FileNotFoundException, IOException {
		return directoryCache.getValue(input, filter, transformer);
	}
}
