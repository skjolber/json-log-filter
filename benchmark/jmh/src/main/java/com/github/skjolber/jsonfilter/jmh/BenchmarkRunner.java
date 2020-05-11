package com.github.skjolber.jsonfilter.jmh;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.jmh.fileutils.FileDirectoryCache;
import com.github.skjolber.jsonfilter.jmh.fileutils.FileDirectoryValue;

public class BenchmarkRunner {
	
	private List<FileDirectoryValue> directories;
	private JsonFilter jsonFilter;

	private StringBuilder builder = new StringBuilder(256 * 1000);

	public BenchmarkRunner(File file, boolean recursive, JsonFilter filter) throws IOException {
		this(file, recursive);
		
		setJsonFilter(filter);
	}

	public BenchmarkRunner(File file, boolean recursive) throws IOException {
		directories = new FileDirectoryCache().getValue(file, new FileFilter() {
			
			@Override
			public boolean accept(File file) {
				return file.getName().toLowerCase().endsWith(".json");
			}
		}, recursive);
	}

	public JsonFilter getJsonFilter() {
		return jsonFilter;
	}

	public void setJsonFilter(JsonFilter filter) {
		this.jsonFilter = filter;
	}

	public long benchmark() {

		// warmup
		long sizeSum = 0;
		for(FileDirectoryValue directory : directories) {
			
			for(int i = 0; i < directory.size(); i++) {
				char[] chars = directory.getValue(i);

				if(jsonFilter.process(chars, 0, chars.length, builder)) {
					sizeSum += builder.length();
				} else {
					throw new RuntimeException("Unable to filter using " + jsonFilter + " for source " + directory.getFile(i));
				}
				
				// reset builder for next iteration
				builder.setLength(0);
			}
		}
		if(sizeSum == 0) {
			throw new IllegalArgumentException();
		}
		return sizeSum;
	}
}
