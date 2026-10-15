package com.github.skjolber.jsonfilter.jmh.tmp;

import java.io.File;
import java.io.IOException;
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

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.pp.Indent;
import com.github.skjolber.jsonfilter.core.pp.PrettyPrintingJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxStringLengthRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.PathMaxStringLengthRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.jmh.BenchmarkRunner;

/**
 * Not part of the benchmark module: run.sh copies it into both builds it compares. Measures whitespace-removing filters on pretty-printed input, and the pretty printer.
 */

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class TmpPrettyInputBenchmark {

	private static final String[] ANON = new String[] {"/CVE_Items/cve/affects/vendor/vendor_data/vendor_name"};
	private static final String[] PRUNE = new String[] {"/CVE_Items/cve/references", "//version"};

	@Param(value={"2KB", "22KB", "200KB"})
	private String fileName;

	private BenchmarkRunner<JsonFilter> pathRemoveWhitespace;
	private BenchmarkRunner<JsonFilter> pathMaxSizeRemoveWhitespace;
	private BenchmarkRunner<JsonFilter> maxStringLengthRemoveWhitespace;
	private BenchmarkRunner<JsonFilter> prettyPrint;

	@Setup
	public void init() throws Exception {
		File file = new File("./src/test/resources/benchmark/cves/" + fileName);
		long minimum = Integer.MAX_VALUE;
		for (File f : file.listFiles()) {
			minimum = Math.min(minimum, f.length());
		}
		pathRemoveWhitespace = new BenchmarkRunner<>(file, true, new PathMaxStringLengthRemoveWhitespaceJsonFilter(64, -1, ANON, PRUNE), false, true);
		pathMaxSizeRemoveWhitespace = new BenchmarkRunner<>(file, true, new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(64, (int)(minimum / 2), -1, ANON, PRUNE), false, true);
		maxStringLengthRemoveWhitespace = new BenchmarkRunner<>(file, true, new MaxStringLengthRemoveWhitespaceJsonFilter(64), false, true);
		prettyPrint = new BenchmarkRunner<>(file, true, new PrettyPrintingJsonFilter(Indent.newBuilder().build()), false, false);
	}

	@Benchmark
	public long path_remove_whitespace_bytes() throws IOException {
		return pathRemoveWhitespace.benchmarkBytes();
	}

	@Benchmark
	public long path_remove_whitespace_char() throws IOException {
		return pathRemoveWhitespace.benchmarkCharacters();
	}

	@Benchmark
	public long path_max_size_remove_whitespace_bytes() throws IOException {
		return pathMaxSizeRemoveWhitespace.benchmarkBytes();
	}

	@Benchmark
	public long path_max_size_remove_whitespace_char() throws IOException {
		return pathMaxSizeRemoveWhitespace.benchmarkCharacters();
	}

	@Benchmark
	public long max_string_length_remove_whitespace_bytes() throws IOException {
		return maxStringLengthRemoveWhitespace.benchmarkBytes();
	}

	@Benchmark
	public long pretty_print_bytes() throws IOException {
		return prettyPrint.benchmarkBytes();
	}

	@Benchmark
	public long pretty_print_char() throws IOException {
		return prettyPrint.benchmarkCharacters();
	}
}
