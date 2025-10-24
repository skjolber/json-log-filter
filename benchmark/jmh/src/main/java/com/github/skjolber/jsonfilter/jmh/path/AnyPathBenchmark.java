package com.github.skjolber.jsonfilter.jmh.path;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
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
import com.github.skjolber.jsonfilter.base.path.any.AnyPathFilter;
import com.github.skjolber.jsonfilter.base.path.any.AnyPathFilters;
import com.github.skjolber.jsonfilter.core.AnyPathJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.SingleFullPathJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMultiAnyPathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonSingleFullPathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jmh.utils.JsonMaskerJsonFilter;

import dev.blaauwendraad.masker.json.JsonMasker;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;


@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class AnyPathBenchmark {

	private String[] KEYS = new String[] {"name", "first_name", "last_name", "address1", "address2", "latitude", "longitude", "phone", "email", "user_id",
			"subtotal_price", "token", "cart_token", "checkout_token", "admin_graphql_api_id", "id", "code", "po_number", "zip", "city"
	};
	
	@Param(value={"1","2","3","4","5","6","7","8","9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20"})
	//@Param(value={"1","5", "10", "15", "20"})
	private String count; 

	private List<char[]> chars;
	private List<byte[]> bytes;
	private AnyPathFilters anyPathFilters;
	
	@Setup
	public void init() throws Exception {
		File file = new File("./src/test/resources/benchmark/shopify/order.json");
		
		List<String> list = ExtractFieldNames.extract(IOUtils.toString(file.toURI(), StandardCharsets.UTF_8));
		if(list.size() < 100) {
			throw new RuntimeException();
		}

		this.chars = new ArrayList<char[]>();
		this.bytes = new ArrayList<byte[]>();
		
		for (String string : list) {
			chars.add(string.toCharArray());
			bytes.add(string.getBytes(StandardCharsets.UTF_8));
		}
		
		List<AnyPathFilter> keys = new ArrayList<>();
		int count = Integer.parseInt(this.count);
		for(int i = 0; i < count; i++) {
			keys.add(AnyPathFilter.create(KEYS[i], FilterType.ANON));
		}
		
		anyPathFilters = AnyPathFilters.create(keys);
	}

	@Benchmark
	public long jsonLogFilterChars() throws IOException {
		int count = 0;
		
		for(char[] str : chars) {
			if(anyPathFilters.matchPath(str, 0, str.length) != null) {
				count++;
			}
		}
		
		return count;
	}

	@Benchmark
	public long jsonLogFilterBytes() throws IOException {
		int count = 0;
		
		for(byte[] str : bytes) {
			if(anyPathFilters.matchPath(str, 0, str.length) != null) {
				count++;
			}
		}
		
		return count;
	}
	
	@Benchmark
	public long jsonLogFilterSetChars() throws IOException {
		int count = 0;
		
		for(char[] str : chars) {
			if(anyPathFilters.matchPath(new String(str, 0, str.length)) != null) {
				count++;
			}
		}
		
		return count;
	}

	
	@Benchmark
	public long jsonLogFilterSetBytes() throws IOException {
		int count = 0;
		
		for(byte[] str : bytes) {
			if(anyPathFilters.matchPath(new String(str, 0, str.length)) != null) {
				count++;
			}
		}
		
		return count;
	}
	
	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(AnyPathBenchmark.class.getSimpleName())
				.warmupIterations(1)
				.measurementIterations(1)
				.forks(1)
				.resultFormat(ResultFormatType.JSON)
				.result("target/" + System.currentTimeMillis() + ".json")
				.build();

		new Runner(opt).run();
	}
}