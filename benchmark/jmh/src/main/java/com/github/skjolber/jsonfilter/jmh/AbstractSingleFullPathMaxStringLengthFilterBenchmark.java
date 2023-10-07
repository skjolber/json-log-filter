package com.github.skjolber.jsonfilter.jmh;
import java.io.File;
import java.io.IOException;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.SingleFullPathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxStringLengthRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonSingleFullPathMaxStringLengthJsonFilter;


public abstract class AbstractSingleFullPathMaxStringLengthFilterBenchmark {

	protected JacksonBenchmarkRunner jacksonMaxSizeJsonFilter;
	protected BenchmarkRunner<? extends JsonFilter> coreMaxSizeJsonFilter;
	protected BenchmarkRunner<? extends JsonFilter> coreRemoveWhitespaceMaxSizeJsonFilter;
	
	@Setup
	public void init() throws Exception {
		File file = getFile();

		int maxStringLength = getMaxStringLength();
		
		String path = getPath();
		FilterType type = getFilterType();
		
		jacksonMaxSizeJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonSingleFullPathMaxStringLengthJsonFilter(maxStringLength, path, type), true, false);
		coreMaxSizeJsonFilter = new BenchmarkRunner<>(file, true, new SingleFullPathMaxStringLengthJsonFilter(maxStringLength, -1, path, type), true, false);
		coreRemoveWhitespaceMaxSizeJsonFilter = new BenchmarkRunner<>(file, true, new SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(maxStringLength, -1, path, type), true, false);
	}
	
	protected abstract FilterType getFilterType();

	protected abstract String getPath();

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