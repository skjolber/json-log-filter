package com.github.skjolber.jsonfilter.jmh;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
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
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.core.AnyPathJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.MultiFullPathJsonFilter;
import com.github.skjolber.jsonfilter.core.MultiPathJsonFilter;
import com.github.skjolber.jsonfilter.core.MultiPathMaxSizeMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.MultiPathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.SingleAnyPathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.SingleFullPathJsonFilter;
import com.github.skjolber.jsonfilter.core.SingleFullPathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMultiAnyPathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMultiPathMaxSizeMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMultiPathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonSingleFullPathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jmh.filter.PrimitiveJsonPropertyBodyFilter;
import com.github.skjolber.jsonfilter.jmh.utils.ArakelianJsonFilterJsonFilter;
import com.github.skjolber.jsonfilter.jmh.utils.JsonMaskerJsonFilter;

import dev.blaauwendraad.masker.json.JsonMasker;


@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 15, time = 1, timeUnit = TimeUnit.SECONDS)

@Fork(1)
public class AllFilterBenchmark {

	public static final String DEFAULT_XPATH = "/address";
	public static final String DEFAULT_ANY_XPATH = "//address";

	private JacksonBenchmarkRunner jacksonMultiPathMaxSizeMaxStringLengthJsonFilter;
	private BenchmarkRunner<JacksonJsonFilter> singleFullPathAnonymizeMaxStringLengthJacksonJsonFilter;
	private BenchmarkRunner<JsonFilter> anyPathJsonMaskerJsonFilter;
	private BenchmarkRunner<JsonFilter> singlePathArakelianJsonFilter;

	private BenchmarkRunner<JacksonJsonFilter> maxStringLengthJacksonJsonFilter;
	private BenchmarkRunner<JacksonJsonFilter> maxSizeJacksonJsonFilter;

	private BenchmarkRunner<JsonFilter> defaultJsonFilter;

	private BenchmarkRunner<JsonFilter> singleFullPathAnonymizeJsonFilter;
	private BenchmarkRunner<JsonFilter> maxStringLengthJsonFilter;
	private BenchmarkRunner<JsonFilter> maxSizeJsonFilter;
	
	private BenchmarkRunner<JsonFilter> singleFullPathMaxStringLengthAnonymizeJsonFilter;

	private BenchmarkRunner<JsonFilter> multiPathAnonymizeJsonFilter;
	private BenchmarkRunner<JsonFilter> multiPathAnonymizeFullPathJsonFilter;
	private BenchmarkRunner<JsonFilter> multiPathMaxStringLengthAnonymizeJsonFilter;

	private BenchmarkRunner<JacksonJsonFilter> multiPathAnonymizeMaxStringLengthJacksonJsonFilter;
	private BenchmarkRunner<JacksonJsonFilter> multiAnyPathAnonymizeMaxStringLengthJacksonJsonFilter;
	private BenchmarkRunner<JsonFilter> multiAnyPathLogbookJsonFilter;
	private BenchmarkRunner<JsonFilter> anyPathAnonymizeJsonFilter;
	private BenchmarkRunner<JsonFilter> singleAnyPathAnonymizeMaxStringLengthJsonFilter;

	private BenchmarkRunner<JsonFilter> multiPathAnonymizeMaxSizeMaxStringLengthJsonFilter;
	
	private final boolean prettyPrinted = false;

	@Setup
	public void init() throws Exception {
		File file = new File("./src/test/resources/benchmark/simple");

		String xpath = DEFAULT_XPATH;

		// passthrough
		defaultJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, prettyPrinted);
		defaultJsonFilter.setJsonFilter(new DefaultJsonFilter());

		// generic filters
		// jackson
		jacksonMultiPathMaxSizeMaxStringLengthJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(20, -1, new String[] {xpath}, null), prettyPrinted);
		singleFullPathAnonymizeMaxStringLengthJacksonJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonSingleFullPathMaxStringLengthJsonFilter(20, xpath, FilterType.ANON), prettyPrinted);
		multiPathAnonymizeMaxStringLengthJacksonJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonMultiPathMaxStringLengthJsonFilter(20, new String[] {xpath}, null), prettyPrinted);
		multiAnyPathAnonymizeMaxStringLengthJacksonJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonMultiAnyPathMaxStringLengthJsonFilter(20, new String[] {DEFAULT_ANY_XPATH}, null), prettyPrinted);
		maxStringLengthJacksonJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonMaxStringLengthJsonFilter(20), prettyPrinted);
		maxSizeJacksonJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonMaxSizeJsonFilter(128), prettyPrinted);
		
		// json-log-filter
		maxStringLengthJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new MaxStringLengthJsonFilter(20), prettyPrinted);
		maxSizeJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new MaxSizeJsonFilter(128), prettyPrinted);

		// path - single
		singleFullPathAnonymizeJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new SingleFullPathJsonFilter(-1, xpath, FilterType.ANON), prettyPrinted);
		singleFullPathMaxStringLengthAnonymizeJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new SingleFullPathMaxStringLengthJsonFilter(20, -1, xpath, FilterType.ANON), prettyPrinted);
		anyPathAnonymizeJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new AnyPathJsonFilter(-1, new String[]{xpath}, null), prettyPrinted);
		singleAnyPathAnonymizeMaxStringLengthJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new SingleAnyPathMaxStringLengthJsonFilter(20, -1, DEFAULT_ANY_XPATH, FilterType.ANON), prettyPrinted);
		
		// path - multiple
		multiPathAnonymizeJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new MultiPathJsonFilter(-1, new String[]{xpath}, null), prettyPrinted);
		multiPathMaxStringLengthAnonymizeJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new MultiPathMaxStringLengthJsonFilter(20, -1, new String[]{xpath}, null), prettyPrinted);
		multiPathAnonymizeFullPathJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new MultiFullPathJsonFilter(-1, new String[]{xpath}, null), prettyPrinted);

		// max size
		multiPathAnonymizeMaxSizeMaxStringLengthJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new MultiPathMaxSizeMaxStringLengthJsonFilter(20, -1, -1, new String[]{xpath}, null), prettyPrinted);

		// other
		Set<String> hashSet = new HashSet<>();
		hashSet.add("product_name");
		JsonMasker masker = JsonMasker.getMasker(hashSet);
		
		anyPathJsonMaskerJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new JsonMaskerJsonFilter(masker), false);
		singlePathArakelianJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new ArakelianJsonFilterJsonFilter(DEFAULT_XPATH), prettyPrinted);

		multiAnyPathLogbookJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, PrimitiveJsonPropertyBodyFilter.replaceString((a) -> a.equals("firstName"), "*****"), prettyPrinted);
	}

	@Benchmark
	public long noop_passthrough() throws IOException {
		return defaultJsonFilter.benchmarkBytes();
	}
	
	@Benchmark
	public long multiAnyPathLogbook() throws IOException {
		return multiAnyPathLogbookJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long anonSingleMaxStringLengthJackson() throws IOException {
		return singleFullPathAnonymizeMaxStringLengthJacksonJsonFilter.benchmarkBytes();
	}
	
	@Benchmark
	public long anonMultiMaxStringLengthJackson() throws IOException {
		return multiPathAnonymizeMaxStringLengthJacksonJsonFilter.benchmarkBytes();
	}
	
	@Benchmark
	public long jacksonMultiPathMaxSizeMaxStringLengthJsonFilter() throws IOException {
		return jacksonMultiPathMaxSizeMaxStringLengthJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long anonMultiAnyMaxStringLengthJackson() throws IOException {
		return multiAnyPathAnonymizeMaxStringLengthJacksonJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long maxStringLengthJackson() throws IOException {
		return maxStringLengthJacksonJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long maxSizeJackson() throws IOException {
		return maxSizeJacksonJsonFilter.benchmarkBytes();
	}
	
	@Benchmark
	public long anonSingleFullPath() throws IOException {
		return singleFullPathAnonymizeJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long anonAnyPath() throws IOException {
		return anyPathAnonymizeJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long anonSingleAnyPathJsonMask() throws IOException {
		return anyPathJsonMaskerJsonFilter.benchmarkBytesAsArray();
	}

	@Benchmark
	public long anonSingleAnyPathMaxStringLength() throws IOException {
		return singleAnyPathAnonymizeMaxStringLengthJsonFilter.benchmarkBytes();
	}
	
	@Benchmark
	public long anonSingleFullPathMaxStringLength() throws IOException {
		return singleFullPathMaxStringLengthAnonymizeJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long anonMultiPathMaxStringLength() throws IOException {
		return multiPathMaxStringLengthAnonymizeJsonFilter.benchmarkBytes();
	}
	
	@Benchmark
	public long multiPathAnonymizeMaxSizeMaxStringLengthJsonFilter() throws IOException {
		return multiPathAnonymizeMaxSizeMaxStringLengthJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long anonMultiPath() throws IOException {
		return multiPathAnonymizeJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long anonMultiFullPath() throws IOException {
		return multiPathAnonymizeFullPathJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long maxStringLength() throws IOException {
		return maxStringLengthJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long maxSize() throws IOException {
		return maxSizeJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long arakelian_filter() throws IOException {
		return singlePathArakelianJsonFilter.benchmarkBytes();
	}
	
	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(AllFilterBenchmark.class.getSimpleName())
				.warmupIterations(25)
				.measurementIterations(50)
				.resultFormat(ResultFormatType.JSON)
				.result("target/" + System.currentTimeMillis() + ".json")
				.build();

		new Runner(opt).run();
	}
}