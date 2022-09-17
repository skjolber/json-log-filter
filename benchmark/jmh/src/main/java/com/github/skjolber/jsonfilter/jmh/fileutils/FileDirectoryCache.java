package com.github.skjolber.jsonfilter.jmh.fileutils;

import static com.github.skjolber.jsonfilter.jmh.fileutils.FileUtils.getResourcesAsFile;
import static com.github.skjolber.jsonfilter.jmh.fileutils.FileUtils.read;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class FileDirectoryCache {

	private Map<FileDirectoryKey, FileDirectoryValue> map = new ConcurrentHashMap<FileDirectoryKey, FileDirectoryValue>();
	
	public FileDirectoryValue getValue(String input, final FileFilter filter, Function<String, String> transformer) throws IOException {
		FileDirectoryKey key = new FileDirectoryKey(input, filter);
		if(map.containsKey(key)) {
			return map.get(key);
		}
		
		FileDirectoryValue value = getValue(getResourcesAsFile(input), filter, transformer);
		
		map.put(key, value);
		
		return value;
	}

	public FileDirectoryValue getValue(File sourceDirectory, final FileFilter filter, Function<String, String> transformer) throws IOException {

		File[] sourceFiles = sourceDirectory.listFiles(filter);
		if(sourceFiles == null) {
			throw new IllegalArgumentException("No files in " + sourceDirectory);
		}
		
		Arrays.sort(sourceFiles);
		
		String[] inputValues = new String[sourceFiles.length];
		for(int i = 0; i < sourceFiles.length; i++) {
			inputValues[i] = transformer.apply(read(sourceFiles[i]));
		}
		
		FileDirectoryValue value = new FileDirectoryValue(sourceDirectory, sourceFiles, inputValues);
		
		return value;
	}
	

	public List<FileDirectoryValue> getValue(File sourceDirectory, final FileFilter filter, boolean includeSubdirectories) throws IOException {
		return getValue(sourceDirectory, filter, includeSubdirectories, Function.identity());
	}

	public List<FileDirectoryValue> getValue(File sourceDirectory, final FileFilter filter, boolean includeSubdirectories, Function<String, String> transformer) throws IOException {
		List<FileDirectoryValue> all = new ArrayList<FileDirectoryValue>();
		
		all.add(getValue(sourceDirectory, filter, transformer));

		if(includeSubdirectories) {
			
			int i = 0;
			do {
				File[] subdirectories = all.get(i).getDirectory().listFiles(new FileFilter() {
					
					@Override
					public boolean accept(File dir) {
						return dir.isDirectory();
					}
				});
				
				if(subdirectories != null) {
					for(File subdirectory : subdirectories) {
						FileDirectoryValue value = getValue(subdirectory, filter, transformer);
						if(value.size() > 0) {
							all.add(getValue(subdirectory, filter, transformer));
						}
					}
				}
				
				i++;
			} while(i < all.size());
		}
		
		return all;
	}

}
