package com.github.skjolber.jsonfilter.jmh;
import java.io.File;
import java.io.IOException;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxSizeJsonFilter;


public abstract class AbstractMaxSizeFilterBenchmark {

	protected JacksonBenchmarkRunner jacksonJsonFilter;
	protected BenchmarkRunner<MaxSizeJsonFilter> coreJsonFilter;
	protected BenchmarkRunner<MaxSizeRemoveWhitespaceJsonFilter> coreRemoveWhitespaceJsonFilter;
	
	@Setup
	public void init() throws Exception {
		File file = getFile();

		long minimum = Integer.MAX_VALUE;
		for (File f : file.listFiles()) {
			if(f.length() < minimum) {
				minimum = f.length();
			}
		}

		int maxSize = getMaxSize((int)minimum);

		jacksonJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonMaxSizeJsonFilter(maxSize), true, false);
		coreJsonFilter = new BenchmarkRunner<MaxSizeJsonFilter>(file, true, new MaxSizeJsonFilter(maxSize), true, false);
		coreRemoveWhitespaceJsonFilter = new BenchmarkRunner<MaxSizeRemoveWhitespaceJsonFilter>(file, true, new MaxSizeRemoveWhitespaceJsonFilter(maxSize), true, false);
	}
	
	protected abstract int getMaxSize(int minimum);

	protected abstract File getFile();

	@Benchmark
	public long jackson_bytes() throws IOException {
		return jacksonJsonFilter.benchmarkBytes();
	}
	
	@Benchmark
	public long core_keep_whitespace_bytes() throws IOException {
		return coreJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long core_remove_whitespace_bytes() throws IOException {
		return coreRemoveWhitespaceJsonFilter.benchmarkBytes();
	}
	
	@Benchmark
	public long jackson_char() throws IOException {
		return jacksonJsonFilter.benchmarkCharacters();
	}
	
	@Benchmark
	public long core_keep_whitespace_char() throws IOException {
		return coreJsonFilter.benchmarkCharacters();
	}

	@Benchmark
	public long core_remove_whitespace_char() throws IOException {
		return coreRemoveWhitespaceJsonFilter.benchmarkCharacters();
	}

}