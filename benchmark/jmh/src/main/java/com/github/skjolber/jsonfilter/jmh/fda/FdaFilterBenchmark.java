package com.github.skjolber.jsonfilter.jmh.fda;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.AnyPathJsonFilter;
import com.github.skjolber.jsonfilter.core.DefaultJsonLogFilterBuilder;
import com.github.skjolber.jsonfilter.core.FullPathJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxStringLengthMaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.RemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.jmh.BenchmarkRunner;

/**
 * Benchmarks JSON filtering throughput on OpenFDA drug adverse events data.
 * Used to validate that throughput improvements on CVE data also generalize
 * to other real-world nested JSON structures.
 *
 * Dataset: fda2024.json.gz (OpenFDA drug adverse events, compact JSON)
 * Top-level structure: {"fda_adverse_events": [...]}
 *
 * Filter paths:
 *   anonymize: /fda_adverse_events/patient/drug/medicinalproduct (drug names)
 *   prune:     /fda_adverse_events/patient/drug/openfda (nested classification data)
 *              //reaction (patient reaction arrays)
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class FdaFilterBenchmark {

	private final int maxStringLength = 64;

	private static String[] anon = new String[]{"/fda_adverse_events/patient/drug/medicinalproduct"};
	private static String[] prune = new String[]{"/fda_adverse_events/patient/drug/openfda", "//reaction"};

	private BenchmarkRunner<JsonFilter> maxStringLengthJsonFilter;
	private BenchmarkRunner<JsonFilter> maxStringLengthMaxSizeJsonFilter;
	private BenchmarkRunner<JsonFilter> maxSizeJsonFilter;
	private BenchmarkRunner<JsonFilter> removeWhitespaceJsonFilter;
	private BenchmarkRunner<JsonFilter> anyPathJsonFilter;
	private BenchmarkRunner<JsonFilter> fullPathJsonFilter;
	private BenchmarkRunner<JsonFilter> pathMaxStringLengthJsonFilter;

	@Param(value = {"4KB", "29KB"})
	private String fileName;

	@Setup
	public void init() throws Exception {
		File file = new File("./src/test/resources/benchmark/fda/" + fileName);
		int size = parseKb(fileName) * 1024 - 1;

		maxStringLengthJsonFilter = new BenchmarkRunner<>(file, true,
			new MaxStringLengthJsonFilter(maxStringLength), false);
		maxStringLengthMaxSizeJsonFilter = new BenchmarkRunner<>(file, true,
			new MaxStringLengthMaxSizeJsonFilter(maxStringLength, size), false);
		maxSizeJsonFilter = new BenchmarkRunner<>(file, true,
			new MaxSizeJsonFilter(size), false);
		removeWhitespaceJsonFilter = new BenchmarkRunner<>(file, true,
			new RemoveWhitespaceJsonFilter(), false);
		anyPathJsonFilter = new BenchmarkRunner<>(file, true,
			new AnyPathJsonFilter(size, new String[]{"//medicinalproduct"}, null), false);
		fullPathJsonFilter = new BenchmarkRunner<>(file, true,
			new FullPathJsonFilter(size, anon, null), false);
		pathMaxStringLengthJsonFilter = new BenchmarkRunner<>(file, true,
			new DefaultJsonLogFilterBuilder().withMaxStringLength(maxStringLength)
				.withAnonymize(anon).withPrune(prune).build(), false);
	}

	private static int parseKb(String name) {
		return Integer.parseInt(name.replaceAll("[^0-9]", ""));
	}

	@Benchmark
	public long maxStringLength_core() throws IOException {
		return maxStringLengthJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long maxStringLengthMaxSize_core() throws IOException {
		return maxStringLengthMaxSizeJsonFilter.benchmarkCharacters();
	}

	@Benchmark
	public long maxSize_core() throws IOException {
		return maxSizeJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long core_remove_whitespace() throws IOException {
		return removeWhitespaceJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long anon_any_core() throws IOException {
		return anyPathJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long anon_full_core() throws IOException {
		return fullPathJsonFilter.benchmarkBytesAsArray();
	}

	@Benchmark
	public long all_core() throws IOException {
		return pathMaxStringLengthJsonFilter.benchmarkBytes();
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
			.include(FdaFilterBenchmark.class.getSimpleName())
			.warmupIterations(1)
			.measurementIterations(3)
			.result("target/" + System.currentTimeMillis() + ".FdaFilterBenchmark.json")
			.resultFormat(ResultFormatType.JSON)
			.build();
		new Runner(opt).run();
	}
}
