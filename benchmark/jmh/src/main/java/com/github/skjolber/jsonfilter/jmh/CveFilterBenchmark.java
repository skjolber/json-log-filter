package com.github.skjolber.jsonfilter.jmh;
import java.io.File;
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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.core.MaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.MultiPathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.SingleFullPathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMultiPathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonSinglePathMaxStringLengthJsonFilter;


@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 15, time = 1, timeUnit = TimeUnit.SECONDS)

@Fork(1)
public class CveFilterBenchmark {

	private final int maxStringLength = 64;
	private static String[] anon = new String[] {"/CVE_Items/cve/affects/vendor/vendor_data/vendor_name"};
	private static String[] prune = new String[] {"/CVE_Items/cve/references", "//version"};
	
	private BenchmarkRunner<JsonFilter> multiPathMaxStringLengthJsonFilter;
	private BenchmarkRunner<JacksonJsonFilter> multiPathMaxStringLengthJacksonJsonFilter;
	private BenchmarkRunner<JsonFilter> maxStringLengthJsonFilter;
	private BenchmarkRunner<JacksonJsonFilter> maxStringLengthJacksonJsonFilter;
	
	private BenchmarkRunner<JsonFilter> singlePathMaxStringLengthJsonFilter;
	private BenchmarkRunner<JacksonJsonFilter> singlePathMaxStringLengthJacksonJsonFilter;

	@Param(value={"2KB","8KB","14KB","22KB","30KB","50KB","70KB","100KB","200KB"})
	//@Param(value={"2KB"})

	private String fileName; 
	
	@Setup
	public void init() throws Exception {
		File file = new File("./src/test/resources/benchmark/cves/" + fileName);
		
		multiPathMaxStringLengthJacksonJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonMultiPathMaxStringLengthJsonFilter(maxStringLength, anon, prune));
		multiPathMaxStringLengthJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new MultiPathMaxStringLengthJsonFilter(maxStringLength, -1, anon, prune));

		singlePathMaxStringLengthJacksonJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonSinglePathMaxStringLengthJsonFilter(maxStringLength, anon[0], FilterType.ANON));
		singlePathMaxStringLengthJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new SingleFullPathMaxStringLengthJsonFilter(maxStringLength, -1, anon[0], FilterType.ANON));

		maxStringLengthJacksonJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonMaxStringLengthJsonFilter(maxStringLength));
		maxStringLengthJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new MaxStringLengthJsonFilter(maxStringLength));
	}

	@Benchmark
	public long all_jackson() {
		return multiPathMaxStringLengthJacksonJsonFilter.benchmarkBytes();
	}
	
	@Benchmark
	public long all_core() {
		return multiPathMaxStringLengthJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long maxStringLength_jackson() {
		return maxStringLengthJacksonJsonFilter.benchmarkBytes();
	}
	
	@Benchmark
	public long maxStringLength_core() {
		return maxStringLengthJsonFilter.benchmarkBytes();
	}	

	@Benchmark
	public long anon_single_jackson() {
		return singlePathMaxStringLengthJacksonJsonFilter.benchmarkBytes();
	}
	
	@Benchmark
	public long anon_single_core() {
		return singlePathMaxStringLengthJsonFilter.benchmarkBytes();
	}	
	
	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(CveFilterBenchmark.class.getSimpleName())
				.warmupIterations(5)
				.measurementIterations(5)
				.build();

		new Runner(opt).run();
	}
}