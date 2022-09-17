package com.github.skjolber.jsonfilter.jmh;

import java.io.File;
import java.io.IOException;

import com.github.skjolber.jsonfilter.jackson.JacksonJsonFilter;
import com.github.skjolber.jsonfilter.jmh.fileutils.FileDirectoryValue;

public class JacksonBenchmarkRunner extends BenchmarkRunner<JacksonJsonFilter> {
	
	public JacksonBenchmarkRunner(File file, boolean recursive, JacksonJsonFilter filter, boolean newBuilder, boolean prettyPrint)
			throws IOException {
		super(file, recursive, filter, newBuilder, prettyPrint);
	}

	public JacksonBenchmarkRunner(File file, boolean recursive, JacksonJsonFilter filter, boolean prettyPrint) throws IOException {
		super(file, recursive, filter, prettyPrint);
	}

	public JacksonBenchmarkRunner(File file, boolean recursive, boolean prettyPrint) throws IOException {
		super(file, recursive, prettyPrint);
	}

	public long benchmarkBytes() {

		// warmup
		long sizeSum = 0;
		for(FileDirectoryValue directory : directories) {
			
			for(int i = 0; i < directory.size(); i++) {
				byte[] bytes = directory.getValueAsBytes(i);
				
				StringBuilder builder;
				if(newBuilder) {
					builder = new StringBuilder(bytes.length);
				} else {
					builder = this.builder;
				}
				if(jsonFilter.process(bytes, builder)) {
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
