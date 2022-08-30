package com.github.skjolber.jsonfilter.jmh;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.ws.PrettyPrintJsonFilter;
import com.github.skjolber.jsonfilter.jmh.fileutils.FileDirectoryValue;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 15, time = 1, timeUnit = TimeUnit.SECONDS)

@Fork(1)
public class RemoveWhitespaceBenchmark {
	
	protected StringBuilder builder = new StringBuilder();

	private char[] enabled;
	private char[] disabled;
	
	private JsonFilter test0 = new PrettyPrintJsonFilter();

	@Setup
	public void init() throws Exception {
		File prettyPrinted = new File("/home/skjolber/git/json-log-filter-github/impl/core/src/test/resources/json/wiki/person.prettyprinted.json");

		try (InputStream fin = new FileInputStream(prettyPrinted)) {
			this.enabled = IOUtils.toString(fin, StandardCharsets.UTF_8).toCharArray();
		}
		
		File plain = new File("/home/skjolber/git/json-log-filter-github/impl/core/src/test/resources/json/wiki/person.json");

		try (InputStream fin = new FileInputStream(plain)) {
			disabled = IOUtils.toString(fin, StandardCharsets.UTF_8).toCharArray();
		}

	}
	
	@Benchmark
	public long test() throws IOException {
		return benchmarkBytes(test0, enabled);
	}

	@Benchmark
	public long test0PP() throws IOException {
		return benchmarkBytes(test0, disabled);
	}
	
	
	public long benchmarkBytes(JsonFilter jsonFilter, char[] content) throws IOException {
		int sumSize = 0;
		if(jsonFilter.process(content, 0, content.length, builder)) {
			sumSize += builder.toString().length(); // note: string output
		} else {
			throw new RuntimeException();
		}
		
		// reset builder for next iteration
		builder.setLength(0);
		return sumSize;
	}
	
	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(RemoveWhitespaceBenchmark.class.getSimpleName())
				.warmupIterations(5)
				.measurementIterations(5)
				.build();

		new Runner(opt).run();
	}
}