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
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.core.AnyPathJsonFilter;
import com.github.skjolber.jsonfilter.core.DefaultJsonLogFilterBuilder;
import com.github.skjolber.jsonfilter.core.FullPathJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxStringLengthMaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.PathMaxSizeMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxStringLengthRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.RemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonJsonLogFilterBuilder;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxSizeMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonPathMaxSizeMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jmh.utils.JsonMaskerJsonFilter;

import dev.blaauwendraad.masker.json.JsonMasker;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;


@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)

@Fork(1)
public class CveFilterBenchmark {

	private final int maxStringLength = 64;
	private static String[] anon = new String[] {"/CVE_Items/cve/affects/vendor/vendor_data/vendor_name"};
	private static String[] anon2 = new String[] {"$.CVE_Items[?(@.cve.affects.vendor.vendor_data[?(@.vendor_name)])]"};
	
	private static String[] prune = new String[] {"/CVE_Items/cve/references", "//version"};
	private static String[] prune2 = new String[] {"$.CVE_Items[?(@.cve.references)]", "$..version"};

	private JacksonBenchmarkRunner jacksonPathMaxSizeMaxStringLengthJsonFilter;
	private BenchmarkRunner<JsonFilter> pathMaxSizeMaxStringLengthJsonFilter;

	private BenchmarkRunner<JsonFilter> pathMaxStringLengthJsonFilter;
	private BenchmarkRunner<JacksonJsonFilter> pathMaxStringLengthJacksonJsonFilter;

	private BenchmarkRunner<JsonFilter> anyPathJsonFilter; 
	private BenchmarkRunner<JsonFilter> fullPathJsonFilter; 

	private BenchmarkRunner<JsonFilter> maxStringLengthJsonFilter;
	private BenchmarkRunner<JacksonJsonFilter> maxStringLengthJacksonJsonFilter;
	private BenchmarkRunner<JsonFilter> maxStringLengthRemoveWhitespaceJsonFilter;
	
	private BenchmarkRunner<JsonFilter> maxStringLengthMaxSizeJsonFilter;
	private BenchmarkRunner<JsonFilter> maxStringLengthMaxSizeRemoveWhitespaceJsonFilter;
	private BenchmarkRunner<JacksonJsonFilter> maxStringLengthMaxSizeJacksonJsonFilter;
	
	private JacksonBenchmarkRunner maxSizeJacksonJsonFilter;
	private BenchmarkRunner<JsonFilter> maxSizeJsonFilter; 
	private BenchmarkRunner<JsonFilter> maxSizeRemoveWhitespaceJsonFilter; 
	
	private BenchmarkRunner<JsonFilter> anyPathJsonMaskerJsonFilter;
	private BenchmarkRunner<JsonFilter> fullPathJsonMaskerJsonFilter;

	private BenchmarkRunner<JsonFilter> removeWhitespaceJsonFilter;
	
	//	@Param(value={"2KB","8KB","14KB","22KB","30KB","50KB","70KB","100KB","200KB"})
	@Param(value={"8KB"})
	private String fileName;
	private final boolean prettyPrinted = false;
	
	@Setup
	public void init() throws Exception {
		File file = new File("./src/test/resources/benchmark/cves/" + fileName);

		//BodyFilter maxLength = JsonPathBodyFilters.jsonPath("$..*[?(@.* =~ /^[\\w\\W]{" + maxStringLength + ",}$/)]").replace("..removed");
		int size = Integer.parseInt(fileName.substring(0,  fileName.length() - 2)) * 1024 - 1;
		
		jacksonPathMaxSizeMaxStringLengthJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonPathMaxSizeMaxStringLengthJsonFilter(maxStringLength, size, anon, prune), prettyPrinted);
		pathMaxSizeMaxStringLengthJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new PathMaxSizeMaxStringLengthJsonFilter(maxStringLength, size, -1, anon, prune), prettyPrinted);
		
		pathMaxStringLengthJacksonJsonFilter = new JacksonBenchmarkRunner(file, true, (JacksonJsonFilter) new JacksonJsonLogFilterBuilder().withMaxStringLength(maxStringLength).withAnonymize(anon).withPrune(prune).build(), prettyPrinted);
		pathMaxStringLengthJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new DefaultJsonLogFilterBuilder().withMaxStringLength(maxStringLength).withAnonymize(anon).withPrune(prune).build(), prettyPrinted);

		maxStringLengthJacksonJsonFilter = new JacksonBenchmarkRunner(file, true, (JacksonJsonFilter) new JacksonJsonLogFilterBuilder().withMaxStringLength(maxStringLength).build(), prettyPrinted);
		maxStringLengthJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new MaxStringLengthJsonFilter(maxStringLength), prettyPrinted);
		maxStringLengthRemoveWhitespaceJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new MaxStringLengthRemoveWhitespaceJsonFilter(maxStringLength), prettyPrinted);
		
		maxSizeJacksonJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonMaxSizeJsonFilter(size), prettyPrinted);
		maxSizeJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new MaxSizeJsonFilter(size), prettyPrinted);
		maxSizeRemoveWhitespaceJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new MaxSizeRemoveWhitespaceJsonFilter(size), prettyPrinted);
		
		maxStringLengthMaxSizeJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new MaxStringLengthMaxSizeJsonFilter(maxStringLength, size), prettyPrinted);
		maxStringLengthMaxSizeRemoveWhitespaceJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(maxStringLength, size), prettyPrinted);
		maxStringLengthMaxSizeJacksonJsonFilter = new BenchmarkRunner<JacksonJsonFilter>(file, true, new JacksonMaxSizeMaxStringLengthJsonFilter(maxStringLength, size), prettyPrinted);
		
		removeWhitespaceJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new RemoveWhitespaceJsonFilter(), prettyPrinted);
		
		anyPathJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new AnyPathJsonFilter(size, new String[] {"//version"}, null), prettyPrinted);
		fullPathJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new FullPathJsonFilter(size, anon, null), prettyPrinted);
		
		// other filters
		var singlePathJsonMasker = JsonMasker.getMasker(
	        JsonMaskingConfig.builder()
	                .maskJsonPaths(Set.of("$.CVE_Items.cve.affects.vendor.vendor_data.vendor_name"))
	                .build()
		);
		
		anyPathJsonMaskerJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new JsonMaskerJsonFilter(JsonMasker.getMasker(Set.of("version"))), false);
		fullPathJsonMaskerJsonFilter = new BenchmarkRunner<JsonFilter>(file, true, new JsonMaskerJsonFilter(singlePathJsonMasker), false);
	}
	
	@Benchmark
	public long all_maxsize_jackson() throws IOException {
		return jacksonPathMaxSizeMaxStringLengthJsonFilter.benchmarkBytes();
	}
	
	@Benchmark
	public long all_maxsize_core() throws IOException {
		return pathMaxSizeMaxStringLengthJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long all_jackson() throws IOException {
		return pathMaxStringLengthJacksonJsonFilter.benchmarkBytes();
	}
	
	@Benchmark
	public long all_core() throws IOException {
		return pathMaxStringLengthJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long maxStringLength_jackson() throws IOException {
		return maxStringLengthJacksonJsonFilter.benchmarkBytes();
	}
	
	@Benchmark
	public long maxStringLength_core() throws IOException {
		return maxStringLengthJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long maxStringLength_core_remove_whitespace() throws IOException {
		return maxStringLengthRemoveWhitespaceJsonFilter.benchmarkBytes();
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
	public long maxSize_core_remove_whitespace() throws IOException {
		return maxSizeRemoveWhitespaceJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long maxSizeJackson() {
		return maxSizeJacksonJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long maxStringLengthMaxSize_core() throws IOException {
		return maxStringLengthMaxSizeJsonFilter.benchmarkCharacters();
	}

	@Benchmark
	public long maxStringLengthMaxSize_remove_whitespace_core() throws IOException {
		return maxStringLengthMaxSizeRemoveWhitespaceJsonFilter.benchmarkCharacters();
	}

	@Benchmark
	public long maxStringLengthMaxSize_jackson() throws IOException {
		return maxStringLengthMaxSizeJacksonJsonFilter.benchmarkCharacters();
	}

	@Benchmark
	public long anon_any_core() throws IOException {
		return anyPathJsonFilter.benchmarkBytes();
	}

	@Benchmark
	public long anon_any_jsonMask() throws IOException {
		return anyPathJsonMaskerJsonFilter.benchmarkBytesAsArray();
	}

	@Benchmark
	public long anon_full_jsonMask() throws IOException {
		return fullPathJsonMaskerJsonFilter.benchmarkBytesAsArray();
	}
	
	@Benchmark
	public long anon_full_core() throws IOException {
		return fullPathJsonFilter.benchmarkBytesAsArray();
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(CveFilterBenchmark.class.getSimpleName())
				.warmupIterations(5)
				.measurementIterations(5)
				.result("target/" + System.currentTimeMillis() + ".json")
				.resultFormat(ResultFormatType.JSON)
				.build();

		new Runner(opt).run();
	}
}