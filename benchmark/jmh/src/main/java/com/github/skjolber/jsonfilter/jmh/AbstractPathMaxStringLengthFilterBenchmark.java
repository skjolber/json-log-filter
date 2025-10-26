package com.github.skjolber.jsonfilter.jmh;
import java.io.File;
import java.io.IOException;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.PathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.PathMaxStringLengthRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonPathMaxStringLengthJsonFilter;


public abstract class AbstractPathMaxStringLengthFilterBenchmark {

	protected JacksonBenchmarkRunner jacksonJsonFilter;
	protected BenchmarkRunner<? extends JsonFilter> coreJsonFilter;
	protected BenchmarkRunner<? extends JsonFilter> coreRemoveWhitespaceJsonFilter;
	
	@Setup
	public void init() throws Exception {
		File file = getFile();

		int maxStringLength = getMaxStringLength();

		String[] anon = getAnon();
		String[] prune = getPrune();
		
		jacksonJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonPathMaxStringLengthJsonFilter(maxStringLength, anon, prune), true, false);
		coreJsonFilter = new BenchmarkRunner<>(file, true, new PathMaxStringLengthJsonFilter(maxStringLength, -1, anon, prune), true, false);
		coreRemoveWhitespaceJsonFilter = new BenchmarkRunner<>(file, true, new PathMaxStringLengthRemoveWhitespaceJsonFilter(maxStringLength, -1, anon, prune), true, false);
	}
	
	protected abstract String[] getPrune();

	protected abstract String[] getAnon();

	protected abstract int getMaxStringLength();

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