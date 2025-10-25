package com.github.skjolber.jsonfilter.jmh.path;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

import com.github.skjolber.jsonfilter.core.AnyPathJsonFilter;

import dev.blaauwendraad.masker.json.JsonMasker;


@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class AnyPathFilterBenchmark {

	private String[] KEYS = new String[] {"name", "first_name", "last_name", "address1", "address2", "latitude", "longitude", "phone", "email", "user_id",
			"subtotal_price", "token", "cart_token", "checkout_token", "admin_graphql_api_id", "id", "code", "po_number", "zip", "city"
	};
	
	//@Param(value={"1","2","3","4","5","6","7","8","9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20"})
	@Param(value={"1","5", "10", "15", "20"})
	private String count; 

	private AnyPathJsonFilter anyPathJsonFilter;
	private JsonMasker jsonMaskerJsonFilter;
	
	private byte[] bytes;
	private char[] chars;
	private String string;
	
	@Setup
	public void init() throws Exception {
		File file = new File("./src/test/resources/benchmark/shopify/order.json");
		
		bytes = IOUtils.toByteArray(new FileInputStream(file));
		string = new String(bytes, StandardCharsets.UTF_8);
		chars = string.toCharArray();
		
		List<String> keys = new ArrayList<>();
		List<String> keysWithPrefix = new ArrayList<>();
		
		int count = Integer.parseInt(this.count);
		for(int i = 0; i < count; i++) {
			keys.add(KEYS[i]);
			keysWithPrefix.add("//" + KEYS[i]);
		}
		
		jsonMaskerJsonFilter = JsonMasker.getMasker(new HashSet<>(keys));
		anyPathJsonFilter = new AnyPathJsonFilter(-1, keysWithPrefix.toArray(new String[keysWithPrefix.size()]), null);
	}

	@Benchmark
	public String mark_chars() throws IOException {
		return jsonMaskerJsonFilter.mask(string);
	}

	@Benchmark
	public byte[] mark_bytes() throws IOException {
		return jsonMaskerJsonFilter.mask(bytes);
	}
	
	@Benchmark
	public String core_chars() throws IOException {
		return anyPathJsonFilter.process(string);
	}
	
	@Benchmark
	public byte[] core_bytes() throws IOException {
		return anyPathJsonFilter.process(bytes);
	}
	
	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(AnyPathFilterBenchmark.class.getName())
				.warmupIterations(1)
				.measurementIterations(1)
				.forks(1)
				.resultFormat(ResultFormatType.JSON)
				.result("target/" + System.currentTimeMillis() + ".json")
				.build();

		new Runner(opt).run();
	}
}