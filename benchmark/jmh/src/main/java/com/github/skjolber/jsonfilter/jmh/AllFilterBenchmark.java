package com.github.skjolber.jsonfilter.jmh;
import java.io.File;
import java.util.concurrent.TimeUnit;

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

import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.core.MaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.MultiPathJsonFilter;
import com.github.skjolber.jsonfilter.core.MultiPathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.SingleAnyPathJsonFilter;
import com.github.skjolber.jsonfilter.core.SingleAnyPathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.MultiFullPathJsonFilter;
import com.github.skjolber.jsonfilter.core.SingleFullPathJsonFilter;

import com.github.skjolber.jsonfilter.core.SingleFullPathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMultiAnyPathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMultiPathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonSinglePathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jmh.filter.PrimitiveJsonPropertyBodyFilter;
import com.github.skjolber.jsonfilter.jmh.utils.ArakelianJsonFilterJsonFilter;


@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 15, time = 1, timeUnit = TimeUnit.SECONDS)

@Fork(1)
public class AllFilterBenchmark {

	public static final String DEFAULT_XPATH = "/address";
	public static final String DEFAULT_ANY_XPATH = "//address";

	private BenchmarkRunner singleFullPathAnonymizeMaxStringLengthJacksonJsonFilter;
	private BenchmarkRunner singlePathArakelianJsonFilter;
	private BenchmarkRunner maxStringLengthJacksonJsonFilter;

	private BenchmarkRunner defaultJsonFilter;

	private BenchmarkRunner singleFullPathAnonymizeJsonFilter;
	private BenchmarkRunner maxStringLengthJsonFilter;
	
	private BenchmarkRunner singleFullPathMaxStringLengthAnonymizeJsonFilter;

	private BenchmarkRunner multiPathAnonymizeJsonFilter;
	private BenchmarkRunner multiPathAnonymizeFullPathJsonFilter;
	private BenchmarkRunner multiPathMaxStringLengthAnonymizeJsonFilter;

	private BenchmarkRunner multiPathAnonymizeMaxStringLengthJacksonJsonFilter;
	private BenchmarkRunner multiAnyPathAnonymizeMaxStringLengthJacksonJsonFilter;
	private BenchmarkRunner multiAnyPathLogbookJsonFilter;
	private BenchmarkRunner singleAnyPathAnonymizeJsonFilter;
	private BenchmarkRunner singleAnyPathAnonymizeMaxStringLengthJsonFilter;

	@Setup
	public void init() throws Exception {
		File file = new File("./src/test/resources/benchmark/simple");

		String xpath = DEFAULT_XPATH;

		// passthrough
		defaultJsonFilter = new BenchmarkRunner(file, true);
		defaultJsonFilter.setJsonFilter(new DefaultJsonFilter());

		// generic filters
		// json
		singleFullPathAnonymizeMaxStringLengthJacksonJsonFilter = new BenchmarkRunner(file, true, new JacksonSinglePathMaxStringLengthJsonFilter(20, xpath, FilterType.ANON));
		multiPathAnonymizeMaxStringLengthJacksonJsonFilter = new BenchmarkRunner(file, true, new JacksonMultiPathMaxStringLengthJsonFilter(20, new String[] {xpath}, null));
		multiAnyPathAnonymizeMaxStringLengthJacksonJsonFilter = new BenchmarkRunner(file, true, new JacksonMultiAnyPathMaxStringLengthJsonFilter(20, new String[] {DEFAULT_ANY_XPATH}, null));
		maxStringLengthJacksonJsonFilter = new BenchmarkRunner(file, true, new JacksonMaxStringLengthJsonFilter(20));

		// xml-log-filter
		maxStringLengthJsonFilter = new BenchmarkRunner(file, true, new MaxStringLengthJsonFilter(20));

		// path - single
		singleFullPathAnonymizeJsonFilter = new BenchmarkRunner(file, true, new SingleFullPathJsonFilter(xpath, FilterType.ANON));
		singleFullPathMaxStringLengthAnonymizeJsonFilter = new BenchmarkRunner(file, true, new SingleFullPathMaxStringLengthJsonFilter(20, xpath, FilterType.ANON));
		singleAnyPathAnonymizeJsonFilter = new BenchmarkRunner(file, true, new SingleAnyPathJsonFilter(DEFAULT_ANY_XPATH, FilterType.ANON));
		singleAnyPathAnonymizeMaxStringLengthJsonFilter = new BenchmarkRunner(file, true, new SingleAnyPathMaxStringLengthJsonFilter(20, DEFAULT_ANY_XPATH, FilterType.ANON));
		
		// path - multiple
		multiPathAnonymizeJsonFilter = new BenchmarkRunner(file, true, new MultiPathJsonFilter(new String[]{xpath}, null));
		multiPathMaxStringLengthAnonymizeJsonFilter = new BenchmarkRunner(file, true, new MultiPathMaxStringLengthJsonFilter(20, new String[]{xpath}, null));
		multiPathAnonymizeFullPathJsonFilter = new BenchmarkRunner(file, true, new MultiFullPathJsonFilter(new String[]{xpath}, null));
		// other
		singlePathArakelianJsonFilter = new BenchmarkRunner(file, true, new ArakelianJsonFilterJsonFilter(DEFAULT_XPATH));

		multiAnyPathLogbookJsonFilter = new BenchmarkRunner(file, true, PrimitiveJsonPropertyBodyFilter.replaceString((a) -> a.equals("firstName"), "*****"));
	}

	@Benchmark
	public long noop_passthrough() {
		return defaultJsonFilter.benchmark();
	}
	
	@Benchmark
	public long multiAnyPathLogbook() {
		return multiAnyPathLogbookJsonFilter.benchmark();
	}

	@Benchmark
	public long anonSingleMaxStringLengthJackson() {
		return singleFullPathAnonymizeMaxStringLengthJacksonJsonFilter.benchmark();
	}
	
	@Benchmark
	public long anonMultiMaxStringLengthJackson() {
		return multiPathAnonymizeMaxStringLengthJacksonJsonFilter.benchmark();
	}

	@Benchmark
	public long anonMultiAnyMaxStringLengthJackson() {
		return multiAnyPathAnonymizeMaxStringLengthJacksonJsonFilter.benchmark();
	}

	@Benchmark
	public long maxStringLengthJackson() {
		return maxStringLengthJacksonJsonFilter.benchmark();
	}
	
	@Benchmark
	public long anonSingleFullPath() {
		return singleFullPathAnonymizeJsonFilter.benchmark();
	}

	@Benchmark
	public long anonSingleAnyPath() {
		return singleAnyPathAnonymizeJsonFilter.benchmark();
	}

	@Benchmark
	public long anonSingleAnyPathMaxStringLength() {
		return singleAnyPathAnonymizeMaxStringLengthJsonFilter.benchmark();
	}
	
	@Benchmark
	public long anonSingleFullPathMaxStringLength() {
		return singleFullPathMaxStringLengthAnonymizeJsonFilter.benchmark();
	}

	@Benchmark
	public long anonMultiPathMaxStringLength() {
		return multiPathMaxStringLengthAnonymizeJsonFilter.benchmark();
	}

	@Benchmark
	public long anonMultiPath() {
		return multiPathAnonymizeJsonFilter.benchmark();
	}

	@Benchmark
	public long anonMultiFullPath() {
		return multiPathAnonymizeFullPathJsonFilter.benchmark();
	}

	@Benchmark
	public long maxStringLength() {
		return maxStringLengthJsonFilter.benchmark();
	}
	
	@Benchmark
	public long arakelian_filter() {
		return singlePathArakelianJsonFilter.benchmark();
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(AllFilterBenchmark.class.getSimpleName())
				.warmupIterations(25)
				.measurementIterations(50)
				.build();

		new Runner(opt).run();
	}
}