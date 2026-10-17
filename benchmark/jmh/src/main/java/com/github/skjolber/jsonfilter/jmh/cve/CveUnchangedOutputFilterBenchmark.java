package com.github.skjolber.jsonfilter.jmh.cve;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
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
import com.github.skjolber.jsonfilter.core.CompositeMaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.pp.Indent;
import com.github.skjolber.jsonfilter.core.pp.PrettyPrintingJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.RemoveWhitespaceJsonFilter;

/**
 * process(String) and process(byte[]) of filters which find nothing to do, as with most log messages: whitespace
 * removal on documents without whitespace, and the max-size filters on documents which fit. The CVE documents are
 * compacted at setup. Pretty-printed documents and documents which are cut are the comparison.
 */

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class CveUnchangedOutputFilterBenchmark {

	@Param(value={"2KB","22KB","200KB"})
	private String fileName;

	private String[] compact;
	private byte[][] compactBytes;
	private String[] prettyPrinted;
	private byte[][] prettyPrintedBytes;

	private JsonFilter removeWhitespace;
	private JsonFilter maxSizeRemoveWhitespaceFits;
	private JsonFilter maxSizeRemoveWhitespaceCut;
	private JsonFilter maxSizeFits;
	private JsonFilter compositeFits;
	private JsonFilter maxSizeCut;

	@Setup
	public void init() throws Exception {
		File[] files = new File("./src/test/resources/benchmark/cves/" + fileName).listFiles();
		Arrays.sort(files);

		removeWhitespace = new RemoveWhitespaceJsonFilter();
		PrettyPrintingJsonFilter prettyPrinter = new PrettyPrintingJsonFilter(Indent.newBuilder().build());

		compact = new String[files.length];
		compactBytes = new byte[files.length][];
		prettyPrinted = new String[files.length];
		prettyPrintedBytes = new byte[files.length][];
		int min = Integer.MAX_VALUE;
		int max = 0;
		for(int i = 0; i < files.length; i++) {
			String document = new String(Files.readAllBytes(files[i].toPath()), StandardCharsets.UTF_8);
			compact[i] = removeWhitespace.process(document);
			compactBytes[i] = compact[i].getBytes(StandardCharsets.UTF_8);
			prettyPrinted[i] = prettyPrinter.process(compact[i]);
			prettyPrintedBytes[i] = prettyPrinted[i].getBytes(StandardCharsets.UTF_8);
			min = Math.min(min, compactBytes[i].length);
			max = Math.max(max, compactBytes[i].length);
		}

		maxSizeRemoveWhitespaceFits = new MaxSizeRemoveWhitespaceJsonFilter(max);
		maxSizeRemoveWhitespaceCut = new MaxSizeRemoveWhitespaceJsonFilter(min / 2);
		maxSizeFits = new MaxSizeJsonFilter(max);
		compositeFits = new CompositeMaxSizeJsonFilter(max);
		maxSizeCut = new MaxSizeJsonFilter(min / 2);

		for(int i = 0; i < files.length; i++) {
			if(!removeWhitespace.process(compact[i]).equals(compact[i]) || !maxSizeFits.process(compact[i]).equals(compact[i])) {
				throw new IllegalStateException("Expected no change");
			}
			if(removeWhitespace.process(prettyPrinted[i]).equals(prettyPrinted[i]) || maxSizeCut.process(compact[i]).equals(compact[i])) {
				throw new IllegalStateException("Expected a change");
			}
		}
	}

	private static long process(JsonFilter filter, String[] documents) {
		long sizeSum = 0;
		for(String document : documents) {
			sizeSum += filter.process(document).length();
		}
		return sizeSum;
	}

	private static long process(JsonFilter filter, byte[][] documents) {
		long sizeSum = 0;
		for(byte[] document : documents) {
			sizeSum += filter.process(document).length;
		}
		return sizeSum;
	}

	@Benchmark
	public long remove_whitespace_compact_string() {
		return process(removeWhitespace, compact);
	}

	@Benchmark
	public long remove_whitespace_compact_bytes() {
		return process(removeWhitespace, compactBytes);
	}

	@Benchmark
	public long remove_whitespace_pretty_printed_string() {
		return process(removeWhitespace, prettyPrinted);
	}

	@Benchmark
	public long remove_whitespace_pretty_printed_bytes() {
		return process(removeWhitespace, prettyPrintedBytes);
	}

	@Benchmark
	public long max_size_remove_whitespace_fits_string() {
		return process(maxSizeRemoveWhitespaceFits, compact);
	}

	@Benchmark
	public long max_size_remove_whitespace_cut_string() {
		return process(maxSizeRemoveWhitespaceCut, compact);
	}

	@Benchmark
	public long max_size_fits_bytes() {
		return process(maxSizeFits, compactBytes);
	}

	@Benchmark
	public long composite_fits_bytes() {
		return process(compositeFits, compactBytes);
	}

	@Benchmark
	public long max_size_cut_bytes() {
		return process(maxSizeCut, compactBytes);
	}
}
