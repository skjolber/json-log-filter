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
import com.github.skjolber.jsonfilter.core.FullPathJsonFilter;
import com.github.skjolber.jsonfilter.core.PathJsonFilter;
import com.github.skjolber.jsonfilter.core.PathMaxSizeMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.PathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonAnyPathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonPathMaxSizeMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonPathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jmh.filter.PrimitiveJsonPropertyBodyFilter;
import com.github.skjolber.jsonfilter.jmh.utils.JsonMaskerJsonFilter;

import dev.blaauwendraad.masker.json.JsonMasker;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;


@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class AllFilterBenchmark {

	public static final String DEFAULT_XPATH = "/address";
	public static final String DEFAULT_ANY_XPATH = "//address";

	private JacksonBenchmarkRunner pathMaxSizeMaxStringLengthJacksonJsonFilter;
	private BenchmarkRunner<JsonFilter> anyPathJsonMaskerJsonFilter;
	private BenchmarkRunner<JsonFilter> fullPathJsonMaskerJsonFilter;

	private BenchmarkRunner<JacksonJsonFilter> maxStringLengthJacksonJsonFilter;
	private BenchmarkRunner<JacksonJsonFilter> maxSizeJacksonJsonFilter;

	private BenchmarkRunner<JsonFilter> defaultJsonFilter;

	private BenchmarkRunner<JsonFilter> maxStringLengthJsonFilter;
	private BenchmarkRunner<JsonFilter> maxSizeJsonFilter;
	
	private BenchmarkRunner<JsonFilter> pathAnonymizeJsonFilter;
	private BenchmarkRunner<JsonFilter> fullPathAnonymizeJsonFilter;
	private BenchmarkRunner<JsonFilter> pathAnonymizeMaxStringLengthJsonFilter;

	private BenchmarkRunner<JacksonJsonFilter> pathAnonymizeMaxStringLengthJacksonJsonFilter;
	private BenchmarkRunner<JacksonJsonFilter> anyPathAnonymizeMaxStringLengthJacksonJsonFilter;
	private BenchmarkRunner<JsonFilter> anyPathLogbookJsonFilter;
	private BenchmarkRunner<JsonFilter> anyPathAnonymizeJsonFilter;

	private BenchmarkRunner<JsonFilter> pathAnonymizeMaxSizeMaxStringLengthJsonFilter;
	
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
		pathMaxSizeMaxStringLengthJacksonJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonPathMaxSizeMaxStringLengthJsonFilter(20, -1, new String[] {xpath}, null), prettyPrinted);
		pathAnonymizeMaxStringLengthJacksonJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonPathMaxStringLengthJsonFilter(20, new String[] {xpath}, null), prettyPrinted);
		anyPathAnonymizeMaxStringLengthJacksonJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonAnyPathMaxStringLengthJsonFilter(20, new String[] {DEFAULT_ANY_XPATH}, null), prettyPrinted);
		maxStringLengthJacksonJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonMaxStringLengthJsonFilter(20), prettyPrinted);
		maxSizeJacksonJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonMaxSizeJsonFilter(128), prettyPrinted);
		
		// json-log-filter
		maxStringLengthJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new MaxStringLengthJsonFilter(20), prettyPrinted);
		maxSizeJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new MaxSizeJsonFilter(128), prettyPrinted);

		anyPathAnonymizeJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new AnyPathJsonFilter(-1, new String[]{DEFAULT_ANY_XPATH}, null), prettyPrinted);
		
		// path - multiple
		pathAnonymizeJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new PathJsonFilter(-1, new String[]{xpath}, null), prettyPrinted);
		pathAnonymizeMaxStringLengthJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new PathMaxStringLengthJsonFilter(20, -1, new String[]{xpath}, null), prettyPrinted);
		fullPathAnonymizeJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new FullPathJsonFilter(-1, new String[]{xpath}, null), prettyPrinted);

		// max size
		pathAnonymizeMaxSizeMaxStringLengthJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new PathMaxSizeMaxStringLengthJsonFilter(20, -1, -1, new String[]{xpath}, null), prettyPrinted);

		// other filters
		var singlePathJsonMasker = JsonMasker.getMasker(
	        JsonMaskingConfig.builder()
	                .maskJsonPaths(Set.of("$.address"))
	                .build()
		);
		
		anyPathJsonMaskerJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new JsonMaskerJsonFilter(JsonMasker.getMasker(Set.of("address"))), false);
		fullPathJsonMaskerJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new JsonMaskerJsonFilter(singlePathJsonMasker), false);

		anyPathLogbookJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, PrimitiveJsonPropertyBodyFilter.replaceString((a) -> a.equals("address"), "*"), prettyPrinted);

	}

	@Benchmark
	public long noop_passthrough() throws IOException {
		return defaultJsonFilter.benchmarkBytes();
	}
	
	@Benchmark
	public long anyPathLogbook() throws IOException {
		return anyPathLogbookJsonFilter.benchmarkBytes();
	}
	
	@Benchmark
	public long pathAnonymizeMaxStringLengthJackson() throws IOException {
		return pathAnonymizeMaxStringLengthJacksonJsonFilter.benchmarkBytes();
	}
	
	@Benchmark
	public long pathMaxSizeMaxStringLengthJackson() throws IOException {
		return pathMaxSizeMaxStringLengthJacksonJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long anyPathAnonymizeMaxStringLengthJackson() throws IOException {
		return anyPathAnonymizeMaxStringLengthJacksonJsonFilter.benchmarkBytes();
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
	public long anyPathAnonymize() throws IOException {
		return anyPathAnonymizeJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long anyPathJsonMasker() throws IOException {
		return anyPathJsonMaskerJsonFilter.benchmarkBytesAsArray();
	}

	@Benchmark
	public long fullPathJsonMasker() throws IOException {
		return fullPathJsonMaskerJsonFilter.benchmarkBytesAsArray();
	}

	@Benchmark
	public long pathAnonymizeMaxStringLength() throws IOException {
		return pathAnonymizeMaxStringLengthJsonFilter.benchmarkBytes();
	}
	
	@Benchmark
	public long pathAnonymizeMaxSizeMaxStringLength() throws IOException {
		return pathAnonymizeMaxSizeMaxStringLengthJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long pathAnonymize() throws IOException {
		return pathAnonymizeJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long fullPathAnonymize() throws IOException {
		return fullPathAnonymizeJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long maxStringLength() throws IOException {
		return maxStringLengthJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long maxSize() throws IOException {
		return maxSizeJsonFilter.benchmarkBytes();
	}
	
	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(AllFilterBenchmark.class.getSimpleName())
				.warmupIterations(1)
				.measurementIterations(3)
				.resultFormat(ResultFormatType.JSON)
				.result("target/" + System.currentTimeMillis() + ".json")
				.build();

		new Runner(opt).run();
	}
}