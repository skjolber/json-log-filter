package com.github.skjolber.jsonfilter.jmh;

import java.io.File;
import java.io.IOException;

import org.zalando.logbook.BodyFilter;

import com.github.skjolber.jsonfilter.jmh.fileutils.FileDirectoryValue;
import com.github.skjolber.jsonfilter.jmh.utils.LogbookBodyFilter;

public class LogbookBenchmarkRunner extends BenchmarkRunner<LogbookBodyFilter> {
	
	public LogbookBenchmarkRunner(File file, boolean recursive, BodyFilter filter, boolean prettyPrint) throws IOException {
		super(file, recursive, new LogbookBodyFilter(filter), prettyPrint);
	}
	
	// body is always characters within logbook
	
	@Override
	public long benchmarkBytesViaChars() {
		return benchmarkCharacters();
	}
	
	@Override
	public long benchmarkBytes() {
		return benchmarkCharacters();
	}
	
	public long benchmarkCharacters() {

		long sizeSum = 0;
		for(FileDirectoryValue directory : directories) {
			
			for(int i = 0; i < directory.size(); i++) {
				String chars = directory.getValueAsString(i);
				String process = jsonFilter.process(chars);
				
				sizeSum += process.length();
			}
		}
		if(sizeSum == 0) {
			throw new IllegalArgumentException();
		}
		return sizeSum;
	}
	
}
