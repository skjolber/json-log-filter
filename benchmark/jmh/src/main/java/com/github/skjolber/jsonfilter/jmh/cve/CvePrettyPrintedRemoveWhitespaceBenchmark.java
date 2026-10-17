package com.github.skjolber.jsonfilter.jmh.cve;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import com.github.skjolber.jsonfilter.core.pp.Indent;
import com.github.skjolber.jsonfilter.core.pp.PrettyPrintingJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxStringLengthRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.RemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.jmh.BenchmarkRunner;

/**
 * Whitespace removal on pretty-printed input, which is what it is for. The other CVE benchmarks run on compact
 * documents, which have almost no whitespace to remove. Here the CVE documents are indented with two spaces and
 * line feeds by the library's pretty printer at setup. Max size is half the compact document.
 */

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class CvePrettyPrintedRemoveWhitespaceBenchmark {

	@Param(value={"2KB","8KB","14KB","22KB","30KB","50KB","70KB","100KB","200KB"})
	private String fileName;

	private BenchmarkRunner<JsonFilter> removeWhitespace;
	private BenchmarkRunner<JsonFilter> maxStringLength;
	private BenchmarkRunner<JsonFilter> maxSize;
	private BenchmarkRunner<JsonFilter> maxStringLengthMaxSize;

	@Setup
	public void init() throws Exception {
		File compact = new File("./src/test/resources/benchmark/cves/" + fileName);
		File prettyPrinted = new File("./target/cves-pretty-printed/" + fileName);

		int size = prettyPrint(compact, prettyPrinted) / 2;

		removeWhitespace = new BenchmarkRunner<>(prettyPrinted, true, new RemoveWhitespaceJsonFilter(), true, false);
		maxStringLength = new BenchmarkRunner<>(prettyPrinted, true, new MaxStringLengthRemoveWhitespaceJsonFilter(64), true, false);
		maxSize = new BenchmarkRunner<>(prettyPrinted, true, new MaxSizeRemoveWhitespaceJsonFilter(size), true, false);
		maxStringLengthMaxSize = new BenchmarkRunner<>(prettyPrinted, true, new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(64, size), true, false);
	}

	/**
	 * Pretty-print every document in a directory into another.
	 *
	 * @return the length of the shortest compact document
	 */

	static int prettyPrint(File source, File target) throws IOException {
		PrettyPrintingJsonFilter prettyPrinter = new PrettyPrintingJsonFilter(Indent.newBuilder().build());

		target.mkdirs();

		int shortest = Integer.MAX_VALUE;
		for(File file : source.listFiles()) {
			if(file.getName().toLowerCase().endsWith(".json")) {
				String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
				shortest = Math.min(shortest, json.length());

				Files.write(new File(target, file.getName()).toPath(), prettyPrinter.process(json).getBytes(StandardCharsets.UTF_8));
			}
		}
		return shortest;
	}

	@Benchmark
	public long remove_whitespace_char() throws IOException {
		return removeWhitespace.benchmarkCharacters();
	}

	@Benchmark
	public long remove_whitespace_bytes() throws IOException {
		return removeWhitespace.benchmarkBytes();
	}

	@Benchmark
	public long max_string_length_char() throws IOException {
		return maxStringLength.benchmarkCharacters();
	}

	@Benchmark
	public long max_string_length_bytes() throws IOException {
		return maxStringLength.benchmarkBytes();
	}

	@Benchmark
	public long max_size_char() throws IOException {
		return maxSize.benchmarkCharacters();
	}

	@Benchmark
	public long max_size_bytes() throws IOException {
		return maxSize.benchmarkBytes();
	}

	@Benchmark
	public long max_string_length_max_size_char() throws IOException {
		return maxStringLengthMaxSize.benchmarkCharacters();
	}

	@Benchmark
	public long max_string_length_max_size_bytes() throws IOException {
		return maxStringLengthMaxSize.benchmarkBytes();
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(CvePrettyPrintedRemoveWhitespaceBenchmark.class.getSimpleName())
				.warmupIterations(5)
				.measurementIterations(2)
				.result("target/" + System.currentTimeMillis() + "." + CvePrettyPrintedRemoveWhitespaceBenchmark.class.getSimpleName() + ".json")
				.resultFormat(ResultFormatType.JSON)
				.build();
		new Runner(opt).run();
	}
}
