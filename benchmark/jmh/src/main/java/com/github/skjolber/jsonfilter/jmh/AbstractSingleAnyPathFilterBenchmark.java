package com.github.skjolber.jsonfilter.jmh;
import java.io.File;
import java.io.IOException;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.core.SingleAnyPathJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMultiAnyPathMaxStringLengthJsonFilter;

public abstract class AbstractSingleAnyPathFilterBenchmark {

	protected JacksonBenchmarkRunner jacksonJsonFilter;
	protected BenchmarkRunner<? extends JsonFilter> coreJsonFilter;
	
	@Setup
	public void init() throws Exception {
		File file = getFile();
		
		String path = getPath();
		FilterType type = getFilterType();
		
		String[] paths = new String[]{path};
		
		jacksonJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonMultiAnyPathMaxStringLengthJsonFilter(-1, type == FilterType.ANON ? paths : null, type == FilterType.PRUNE ? paths : null), true, false);
		coreJsonFilter = new BenchmarkRunner<>(file, true, new SingleAnyPathJsonFilter(-1, path, type), true, false);
	}
	
	protected abstract FilterType getFilterType();

	protected abstract String getPath();

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
	public long jackson_char() throws IOException {
		return jacksonJsonFilter.benchmarkCharacters();
	}
	
	@Benchmark
	public long core_keep_whitespace_char() throws IOException {
		return coreJsonFilter.benchmarkCharacters();
	}
}