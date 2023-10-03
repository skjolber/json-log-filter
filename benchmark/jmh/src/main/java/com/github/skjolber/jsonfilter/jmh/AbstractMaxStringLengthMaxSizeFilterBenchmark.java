package com.github.skjolber.jsonfilter.jmh;
import java.io.File;
import java.io.IOException;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import com.github.skjolber.jsonfilter.core.MaxStringLengthMaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxSizeMaxStringLengthJsonFilter;


public abstract class AbstractMaxStringLengthMaxSizeFilterBenchmark {

	protected JacksonBenchmarkRunner jacksonMaxSizeJsonFilter;
	protected BenchmarkRunner<MaxStringLengthMaxSizeJsonFilter> coreMaxSizeJsonFilter;
	protected BenchmarkRunner<MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter> coreRemoveWhitespaceMaxSizeJsonFilter;
	
	@Setup
	public void init() throws Exception {
		File file = getFile();

		int maxSize = getMaxSize();
		int maxStringLength = getMaxStringLength();

		jacksonMaxSizeJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonMaxSizeMaxStringLengthJsonFilter(maxStringLength, maxSize), false);
		coreMaxSizeJsonFilter = new BenchmarkRunner<>(file, true, new MaxStringLengthMaxSizeJsonFilter(maxStringLength, maxSize), false);
		coreRemoveWhitespaceMaxSizeJsonFilter = new BenchmarkRunner<>(file, true, new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(maxStringLength, maxSize), false);
	}
	
	protected abstract int getMaxSize();

	protected abstract int getMaxStringLength();

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