package com.github.skjolber.jsonfilter.jmh;
import java.io.File;
import java.io.IOException;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.MultiPathMaxSizeMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMultiPathMaxSizeMaxStringLengthJsonFilter;


public abstract class AbstractMultiPathMaxSizeMaxStringLengthFilterBenchmark {

	protected JacksonBenchmarkRunner jacksonMaxSizeJsonFilter;
	protected BenchmarkRunner<? extends JsonFilter> coreMaxSizeJsonFilter;
	protected BenchmarkRunner<? extends JsonFilter> coreRemoveWhitespaceMaxSizeJsonFilter;
	
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
		
		jacksonMaxSizeJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(maxStringLength, maxSize, anon, prune), true, false);
		coreMaxSizeJsonFilter = new BenchmarkRunner<>(file, true, new MultiPathMaxSizeMaxStringLengthJsonFilter(maxStringLength, maxSize, -1, anon, prune), true, false);
		coreRemoveWhitespaceMaxSizeJsonFilter = new BenchmarkRunner<>(file, true, new MultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(maxStringLength, maxSize, -1, anon, prune), true, false);
	}
	
	protected abstract int getMaxSize(int minimum);

	protected abstract String[] getPrune();

	protected abstract String[] getAnon();

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