package com.github.skjolber.jsonfilter.jmh.cve;

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
import com.github.skjolber.jsonfilter.core.AnyPathJsonFilter;
import com.github.skjolber.jsonfilter.core.PathMaxSizeMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.PathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.PathMaxStringLengthRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.RemoveWhitespaceNewlineStringJsonFilter;
import com.github.skjolber.jsonfilter.jmh.BenchmarkRunner;

/**
 * The path filters on pretty-printed input, as in {@link CvePrettyPrintedRemoveWhitespaceBenchmark}. A whole object is
 * anonymized, so that the filters also skip whitespace inside anonymized and skipped subtrees. Max size is half the
 * compact document.
 */

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class CvePrettyPrintedPathFilterBenchmark {

	private static final String[] ANONYMIZE = new String[] {"/CVE_Items/cve/references"};
	private static final String[] PRUNE = new String[] {"//version"};

	@Param(value={"2KB","8KB","14KB","22KB","30KB","50KB","70KB","100KB","200KB"})
	private String fileName;

	private BenchmarkRunner<JsonFilter> anyPath;
	private BenchmarkRunner<JsonFilter> pathMaxStringLength;
	private BenchmarkRunner<JsonFilter> pathMaxSizeMaxStringLength;
	private BenchmarkRunner<JsonFilter> pathMaxStringLengthRemoveWhitespace;
	private BenchmarkRunner<JsonFilter> pathMaxStringLengthMaxSizeRemoveWhitespace;
	private BenchmarkRunner<JsonFilter> removeWhitespaceNewlineString;

	@Setup
	public void init() throws IOException {
		File compact = new File("./src/test/resources/benchmark/cves/" + fileName);
		File prettyPrinted = new File("./target/cves-pretty-printed/" + fileName);
		int size = CvePrettyPrintedRemoveWhitespaceBenchmark.prettyPrint(compact, prettyPrinted) / 2;

		anyPath = new BenchmarkRunner<>(prettyPrinted, true, new AnyPathJsonFilter(-1, new String[] {"//references"}, null), true, false);
		pathMaxStringLength = new BenchmarkRunner<>(prettyPrinted, true, new PathMaxStringLengthJsonFilter(64, -1, ANONYMIZE, PRUNE), true, false);
		pathMaxSizeMaxStringLength = new BenchmarkRunner<>(prettyPrinted, true, new PathMaxSizeMaxStringLengthJsonFilter(64, size, -1, ANONYMIZE, PRUNE), true, false);
		pathMaxStringLengthRemoveWhitespace = new BenchmarkRunner<>(prettyPrinted, true, new PathMaxStringLengthRemoveWhitespaceJsonFilter(64, -1, ANONYMIZE, PRUNE), true, false);
		pathMaxStringLengthMaxSizeRemoveWhitespace = new BenchmarkRunner<>(prettyPrinted, true, new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(64, size, -1, ANONYMIZE, PRUNE), true, false);
		removeWhitespaceNewlineString = new BenchmarkRunner<>(prettyPrinted, true, new RemoveWhitespaceNewlineStringJsonFilter(), true, false);
	}

	@Benchmark
	public long any_path_char() throws IOException {
		return anyPath.benchmarkCharacters();
	}

	@Benchmark
	public long any_path_bytes() throws IOException {
		return anyPath.benchmarkBytes();
	}

	@Benchmark
	public long path_max_string_length_char() throws IOException {
		return pathMaxStringLength.benchmarkCharacters();
	}

	@Benchmark
	public long path_max_string_length_bytes() throws IOException {
		return pathMaxStringLength.benchmarkBytes();
	}

	@Benchmark
	public long path_max_size_max_string_length_char() throws IOException {
		return pathMaxSizeMaxStringLength.benchmarkCharacters();
	}

	@Benchmark
	public long path_max_size_max_string_length_bytes() throws IOException {
		return pathMaxSizeMaxStringLength.benchmarkBytes();
	}

	@Benchmark
	public long path_remove_whitespace_char() throws IOException {
		return pathMaxStringLengthRemoveWhitespace.benchmarkCharacters();
	}

	@Benchmark
	public long path_remove_whitespace_bytes() throws IOException {
		return pathMaxStringLengthRemoveWhitespace.benchmarkBytes();
	}

	@Benchmark
	public long path_max_size_remove_whitespace_char() throws IOException {
		return pathMaxStringLengthMaxSizeRemoveWhitespace.benchmarkCharacters();
	}

	@Benchmark
	public long path_max_size_remove_whitespace_bytes() throws IOException {
		return pathMaxStringLengthMaxSizeRemoveWhitespace.benchmarkBytes();
	}

	@Benchmark
	public long remove_whitespace_newline_string_char() throws IOException {
		return removeWhitespaceNewlineString.benchmarkCharacters();
	}

	@Benchmark
	public long remove_whitespace_newline_string_bytes() throws IOException {
		return removeWhitespaceNewlineString.benchmarkBytes();
	}
}
