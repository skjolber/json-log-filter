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
import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.json.JsonPathBodyFilters;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.DefaultJsonLogFilterBuilder;
import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonJsonLogFilterBuilder;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxSizeJsonFilter;


@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 15, time = 1, timeUnit = TimeUnit.SECONDS)

@Fork(1)
public class CveFilterBenchmark {

	private final int maxStringLength = 64;
	private static String[] anon = new String[] {"/CVE_Items/cve/affects/vendor/vendor_data/vendor_name"};
	private static String[] anon2 = new String[] {"$.CVE_Items[?(@.cve.affects.vendor.vendor_data[?(@.vendor_name)])]"};
	
	private static String[] prune = new String[] {"/CVE_Items/cve/references", "//version"};
	private static String[] prune2 = new String[] {"$.CVE_Items[?(@.cve.references)]", "$..version"};
	
	private BenchmarkRunner<JsonFilter> multiPathMaxStringLengthJsonFilter;
	private BenchmarkRunner<JacksonJsonFilter> multiPathMaxStringLengthJacksonJsonFilter;
	private LogbookBenchmarkRunner multiPathLogbookJsonFilter;
	
	private BenchmarkRunner<JsonFilter> maxStringLengthJsonFilter;
	private BenchmarkRunner<JacksonJsonFilter> maxStringLengthJacksonJsonFilter;
	
	private BenchmarkRunner<JsonFilter> singlePathMaxStringLengthJsonFilter;
	private BenchmarkRunner<JacksonJsonFilter> singlePathMaxStringLengthJacksonJsonFilter;

	private JacksonBenchmarkRunner maxSizeJacksonJsonFilter;
	private BenchmarkRunner<JsonFilter> maxSizeJsonFilter; 

	private LogbookBenchmarkRunner singlePathLogbookJsonFilter;

	@Param(value={"2KB","8KB","14KB","22KB","30KB","50KB","70KB","100KB","200KB"})
	//@Param(value={"2KB"})

	private String fileName;
	
	@Setup
	public void init() throws Exception {
		File file = new File("./src/test/resources/benchmark/cves/" + fileName);

		BodyFilter anon0 = JsonPathBodyFilters.jsonPath(anon2[0]).replace("*****");
		BodyFilter prune0 = JsonPathBodyFilters.jsonPath(prune2[0]).delete();
		BodyFilter prune1 = JsonPathBodyFilters.jsonPath(prune2[1]).delete();
		
		//BodyFilter maxLength = JsonPathBodyFilters.jsonPath("$..*[?(@.* =~ /^[\\w\\W]{" + maxStringLength + ",}$/)]").replace("..removed");
		
		multiPathMaxStringLengthJacksonJsonFilter = new JacksonBenchmarkRunner(file, true, (JacksonJsonFilter) new JacksonJsonLogFilterBuilder().withMaxStringLength(maxStringLength).withAnonymize(anon).withPrune(prune).build());
		multiPathMaxStringLengthJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new DefaultJsonLogFilterBuilder().withMaxStringLength(maxStringLength).withAnonymize(anon).withPrune(prune).build());
		multiPathLogbookJsonFilter = new LogbookBenchmarkRunner(file, false, anon0.tryMerge(prune0).tryMerge(prune1));

		singlePathMaxStringLengthJacksonJsonFilter = new JacksonBenchmarkRunner(file, true, (JacksonJsonFilter) new JacksonJsonLogFilterBuilder().withMaxStringLength(maxStringLength).withAnonymize(anon[0]).build());
		singlePathMaxStringLengthJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new DefaultJsonLogFilterBuilder().withMaxStringLength(maxStringLength).withAnonymize(anon[0]).build());
		singlePathLogbookJsonFilter = new LogbookBenchmarkRunner(file, false, anon0);

		maxStringLengthJacksonJsonFilter = new JacksonBenchmarkRunner(file, true, (JacksonJsonFilter) new JacksonJsonLogFilterBuilder().withMaxStringLength(maxStringLength).build());
		maxStringLengthJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new DefaultJsonLogFilterBuilder().withMaxStringLength(maxStringLength).build());

		int size = Integer.parseInt(fileName.substring(0,  fileName.length() - 2)) * 1024;
		
		maxSizeJacksonJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonMaxSizeJsonFilter(size));
		maxSizeJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new MaxSizeJsonFilter(size));
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
	public long all_logbook() {
		return multiPathLogbookJsonFilter.benchmarkCharacters();
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
	public long anon_single_logbook() {
		return singlePathLogbookJsonFilter.benchmarkCharacters();
	}

	@Benchmark
	public long anon_single_core() {
		return singlePathMaxStringLengthJsonFilter.benchmarkBytes();
	}	
	
	@Benchmark
	public long maxSize() {
		return maxSizeJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long maxSizeJackson() {
		return maxSizeJacksonJsonFilter.benchmarkBytes();
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