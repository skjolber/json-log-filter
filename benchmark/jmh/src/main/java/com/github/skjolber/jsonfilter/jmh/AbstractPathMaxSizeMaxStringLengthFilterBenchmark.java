package com.github.skjolber.jsonfilter.jmh;
import java.io.File;
import java.io.IOException;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.PathMaxSizeMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonPathMaxSizeMaxStringLengthJsonFilter;


public abstract class AbstractPathMaxSizeMaxStringLengthFilterBenchmark {

	protected JacksonBenchmarkRunner jacksonJsonFilter;
	protected BenchmarkRunner<? extends JsonFilter> coreJsonFilter;
	protected BenchmarkRunner<? extends JsonFilter> coreRemoveWhitespaceJsonFilter;
	
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
		
		int maxStringLength = getMaxStringLength();

		String[] anon = getAnon();
		String[] prune = getPrune();
		
		jacksonJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonPathMaxSizeMaxStringLengthJsonFilter(maxStringLength, maxSize, anon, prune), true, false);
		coreJsonFilter = new BenchmarkRunner<>(file, true, new PathMaxSizeMaxStringLengthJsonFilter(maxStringLength, maxSize, -1, anon, prune), true, false);
		coreRemoveWhitespaceJsonFilter = new BenchmarkRunner<>(file, true, new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(maxStringLength, maxSize, -1, anon, prune), true, false);
	}
	
	protected abstract int getMaxSize(int minimum);

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