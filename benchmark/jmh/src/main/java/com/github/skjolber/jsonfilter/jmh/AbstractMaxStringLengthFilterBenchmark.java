package com.github.skjolber.jsonfilter.jmh;
import java.io.File;
import java.io.IOException;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxStringLengthRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxStringLengthJsonFilter;


public abstract class AbstractMaxStringLengthFilterBenchmark {

	protected JacksonBenchmarkRunner jacksonMaxSizeJsonFilter;
	protected BenchmarkRunner<? extends JsonFilter> coreMaxSizeJsonFilter;
	protected BenchmarkRunner<? extends JsonFilter> coreRemoveWhitespaceMaxSizeJsonFilter;
	
	@Setup
	public void init() throws Exception {
		File file = getFile();

		int maxStringLength = getMaxStringLength();
		
		jacksonMaxSizeJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonMaxStringLengthJsonFilter(maxStringLength), true, false);
		coreMaxSizeJsonFilter = new BenchmarkRunner<>(file, true, new MaxStringLengthJsonFilter(maxStringLength), true, false);
		coreRemoveWhitespaceMaxSizeJsonFilter = new BenchmarkRunner<>(file, true, new MaxStringLengthRemoveWhitespaceJsonFilter(maxStringLength), true, false);
	}
	
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