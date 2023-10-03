package com.github.skjolber.jsonfilter.jmh;
import java.io.File;
import java.io.IOException;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxSizeJsonFilter;


public abstract class AbstractMaxSizeFilterBenchmark {

	protected JacksonBenchmarkRunner jacksonMaxSizeJsonFilter;
	protected BenchmarkRunner<MaxSizeJsonFilter> coreMaxSizeJsonFilter;
	protected BenchmarkRunner<MaxSizeRemoveWhitespaceJsonFilter> coreRemoveWhitespaceMaxSizeJsonFilter;
	
	@Setup
	public void init() throws Exception {
		File file = getFile();

		int maxSize = getMaxSize();

		System.out.flush();
		System.err.println("Init " + file + " size " + maxSize);


		jacksonMaxSizeJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonMaxSizeJsonFilter(maxSize), false);
		coreMaxSizeJsonFilter = new BenchmarkRunner<MaxSizeJsonFilter>(file, true, new MaxSizeJsonFilter(maxSize), false);
		coreRemoveWhitespaceMaxSizeJsonFilter = new BenchmarkRunner<MaxSizeRemoveWhitespaceJsonFilter>(file, true, new MaxSizeRemoveWhitespaceJsonFilter(maxSize), false);
	}
	
	protected abstract int getMaxSize();

	protected abstract File getFile();

	@Benchmark
	public long jackson_bytes() throws IOException {
		return jacksonMaxSizeJsonFilter.benchmarkBytes();
	}
	
	@Benchmark
	public long core_keep_whitespace_bytes() throws IOException {
		return coreMaxSizeJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long core_remove_whitespace_bytes() throws IOException {
		return coreRemoveWhitespaceMaxSizeJsonFilter.benchmarkBytes();
	}
	
	@Benchmark
	public long jackson_char() throws IOException {
		return jacksonMaxSizeJsonFilter.benchmarkCharacters();
	}
	
	@Benchmark
	public long core_keep_whitespace_char() throws IOException {
		return coreMaxSizeJsonFilter.benchmarkCharacters();
	}

	@Benchmark
	public long core_remove_whitespace_char() throws IOException {
		return coreRemoveWhitespaceMaxSizeJsonFilter.benchmarkCharacters();
	}

}