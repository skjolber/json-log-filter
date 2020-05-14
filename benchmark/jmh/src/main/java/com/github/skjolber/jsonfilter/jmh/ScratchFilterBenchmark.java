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
import org.openjdk.jmh.results.format.ResultFormatType;
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


@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 15, time = 1, timeUnit = TimeUnit.SECONDS)

@Fork(1)
public abstract class ScratchFilterBenchmark {

	public static final String DEFAULT_XPATH = "/address";
	public static final String DEFAULT_ANY_XPATH = "//address";

	private BenchmarkRunner original;
	private BenchmarkRunner modified;
	
	@Param(value={"2KB","8KB","14KB","22KB","30KB","50KB","70KB","100KB","200KB"})
	//@Param(value={"2KB"})
	private String fileName; 
	
	@Setup
	public void init() throws Exception {
		File file = new File("./src/test/resources/benchmark/cves/" + fileName);
		//File file = new File("./src/test/resources/benchmark/simpleWithWhitespace");
		//File file = new File("./src/test/resources/benchmark/simple");		

		//String xpath = DEFAULT_XPATH;
		String xpath = "/CVE_Items/cve/affects/vendor/vendor_data/vendor_name";

		// xml-log-filter
		original = new BenchmarkRunner(file, true, new SingleFullPathJsonFilter(-1, xpath, FilterType.ANON));
		modified = new BenchmarkRunner(file, true, new SingleFullPathJsonFilter(-1, xpath, FilterType.ANON));

		/*
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
		*/
	}
	
	
	/*

	@Benchmark
	public long originalSingleFullPathJsonFilter() {
		return originalSingleFullPathJsonFilter.benchmark();
	}
	
	@Benchmark
	public long modifiedSingleFullPathJsonFilter() {
		return modifiedSingleFullPathJsonFilter.benchmark();
	}
	
	*/
	
	@Benchmark
	public long original() {
		return original.benchmark();
	}
	
	@Benchmark
	public long modified() {
		return modified.benchmark();
	}	

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(ScratchFilterBenchmark.class.getSimpleName())
				.warmupIterations(10)
				.measurementIterations(10)
				.resultFormat(ResultFormatType.JSON)
				.build();

		new Runner(opt).run();
	}
}