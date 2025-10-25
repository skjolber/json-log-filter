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
import com.github.skjolber.jsonfilter.core.SingleFullPathJsonFilter;
import com.github.skjolber.jsonfilter.jmh.utils.JsonMaskerJsonFilter;

import dev.blaauwendraad.masker.json.JsonMasker;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;


@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)

// for prototyping

@Fork(1)
public class ScratchFilterBenchmark {

	public static final String DEFAULT_XPATH = "/address";
	public static final String DEFAULT_ANY_XPATH = "//product_name";

	private BenchmarkRunner<JsonFilter> original;
	private BenchmarkRunner<JsonFilter> modified1;
	private BenchmarkRunner<JsonFilter> modified2;
	
	//@Param(value={"2KB","8KB","14KB","22KB","30KB","50KB","70KB","100KB","200KB"})
	@Param(value={"2KB"})
	private String fileName; 
	
	@Setup
	public void init() throws Exception {
		File file = new File("./src/test/resources/benchmark/cves/" + fileName);
		//File file = new File("./src/test/resources/benchmark/simpleWithWhitespace");
		//File file = new File("./src/test/resources/benchmark/simple");		

		//String xpath = DEFAULT_XPATH;
		String xpath = "/CVE_Items/cve/affects/vendor/vendor_data/vendor_name";
		String jsonPath = "$.CVE_Items.cve.affects.vendor.vendor_data.vendor_name";

		int size = (int) (file.length() - 1);
		
		String key = "product_name";
		
		Set<String> hashSet = new HashSet<>();
		hashSet.add(key);
		JsonMasker masker = JsonMasker.getMasker(hashSet);
		
		boolean prettyPrint = false;
		
		original = new BenchmarkRunner<JsonFilter>(file, true, new SingleFullPathJsonFilter(-1, xpath, FilterType.ANON), true, prettyPrint);
		
		var singlePathJsonMasker = JsonMasker.getMasker(
	        JsonMaskingConfig.builder()
	                .maskJsonPaths(Set.of("$.CVE_Items.cve.affects.vendor.vendor_data.vendor_name"))
	                .build()
		);
		
		//modified1 = new BenchmarkRunner<JsonFilter>(file, true, new JsonMaskerJsonFilter(singlePathJsonMasker), false);

		//original = new BenchmarkRunner<JsonFilter>(file, true, new SingleAnyPathJsonFilter(-1, "//" + key, FilterType.ANON), true, prettyPrint);
		modified1 = new BenchmarkRunner<JsonFilter>(file, true, new JsonMaskerJsonFilter(masker), true, prettyPrint);
		original = new BenchmarkRunner<JsonFilter>(file, true, new AnyPathJsonFilter(-1, new String[]{"//" + key}, null), true, prettyPrint);
		//modified2 = new BenchmarkRunner<JsonFilter>(file, true, new JacksonMultiAnyPathMaxStringLengthJsonFilter(-1, new String[] {DEFAULT_ANY_XPATH}, null), true, prettyPrint);
		
		// xml-log-filter
		//original = new BenchmarkRunner<JsonFilter> (file, true, new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(10, size), true);
		//modified1 = new BenchmarkRunner<JsonFilter> (file, true, new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter2(10, size), true);
		//modified2 = new BenchmarkRunner<JsonFilter> (file, true, new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter3(10, size), true);

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

		multiAnyPathLogbookJsonFilter = new BenchmarkRunner(file, true, PrimitiveJsonPropertyBodyFilter.replaceString((a) -> a.equals("firstName"), "*"));
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
	public long jsonLogFilter() throws IOException {
		return original.benchmarkBytesAsArray();
	}
	
	@Benchmark
	public long masker() throws IOException {
		return modified1.benchmarkBytesAsArray();
	}
	
	/*
	@Benchmark
	public long jackson() throws IOException {
		return modified2.benchmarkBytesAsArray();
	}
	*/	
	
	/*
	@Benchmark
	public long modified2() {
		return modified1.benchmarkCharacters();
	}	
*/
	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(ScratchFilterBenchmark.class.getSimpleName())
				.warmupIterations(3)
				.measurementIterations(1)
				.forks(1)
				.resultFormat(ResultFormatType.JSON)
				.result("target/" + System.currentTimeMillis() + ".json")
				.build();

		new Runner(opt).run();
	}
}