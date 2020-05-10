package com.github.skjolber.jsonfilter.utils;

import static com.github.skjolber.jsonfilter.utils.FileUtils.getResourcesAsFile;
import static com.github.skjolber.jsonfilter.utils.FileUtils.read;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileDirectoryCache {

	private Map<FileDirectoryKey, FileDirectoryValue> map = new ConcurrentHashMap<FileDirectoryKey, FileDirectoryValue>();
	
	public FileDirectoryValue getValue(String input, final FileFilter filter) throws IOException {
		FileDirectoryKey key = new FileDirectoryKey(input, filter);
		if(map.containsKey(key)) {
			return map.get(key);
		}
		
		FileDirectoryValue value = getValue(getResourcesAsFile(input), filter);
		
		map.put(key, value);
		
		return value;
	}

	public FileDirectoryValue getValue(File sourceDirectory, final FileFilter filter) throws IOException {

		File[] sourceFiles = sourceDirectory.listFiles(filter);
		if(sourceFiles == null) {
			throw new IllegalArgumentException("No files in " + sourceDirectory);
		}
		
		Arrays.sort(sourceFiles);
		
		String[] inputValues = new String[sourceFiles.length];
		for(int i = 0; i < sourceFiles.length; i++) {
			inputValues[i] = read(sourceFiles[i]);
		}
		
		FileDirectoryValue value = new FileDirectoryValue(sourceDirectory, sourceFiles, inputValues);
		
		return value;
	}
	
	public List<FileDirectoryValue> getValue(File sourceDirectory, final FileFilter filter, boolean includeSubdirectories) throws IOException {
		List<FileDirectoryValue> all = new ArrayList<FileDirectoryValue>();
		
		all.add(getValue(sourceDirectory, filter));

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
						FileDirectoryValue value = getValue(subdirectory, filter);
						if(value.size() > 0) {
							all.add(getValue(subdirectory, filter));
						}
					}
				}
				
				i++;
			} while(i < all.size());
		}
		
		return all;
	}

}
