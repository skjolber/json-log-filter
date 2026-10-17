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

/**
 * The String API, process(String), with max size filters, for documents which fit, as with most log messages,
 * and for documents which are cut.
 */

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class CveProcessStringMaxSizeFilterBenchmark {

	@Param(value={"2KB","22KB","200KB"})
	private String fileName;

	private String[] documents;

	private JsonFilter maxSizeFits;
	private JsonFilter compositeFits;
	private JsonFilter maxSizeCut;
	private JsonFilter compositeCut;

	@Setup
	public void init() throws Exception {
		File[] files = new File("./src/test/resources/benchmark/cves/" + fileName).listFiles();
		Arrays.sort(files);
		documents = new String[files.length];
		int min = Integer.MAX_VALUE;
		int max = 0;
		for(int i = 0; i < files.length; i++) {
			documents[i] = new String(Files.readAllBytes(files[i].toPath()), StandardCharsets.UTF_8);
			min = Math.min(min, documents[i].length());
			max = Math.max(max, documents[i].length());
		}

		maxSizeFits = new MaxSizeJsonFilter(max);
		compositeFits = new CompositeMaxSizeJsonFilter(max);
		maxSizeCut = new MaxSizeJsonFilter(min / 2);
		compositeCut = new CompositeMaxSizeJsonFilter(min / 2);

		for(String document : documents) {
			if(!maxSizeFits.process(document).equals(document) || !compositeFits.process(document).equals(document)) {
				throw new IllegalStateException("Expected no change");
			}
			if(maxSizeCut.process(document).equals(document) || compositeCut.process(document).equals(document)) {
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

	@Benchmark
	public long maxSize_fits() {
		return process(maxSizeFits, documents);
	}

	@Benchmark
	public long composite_fits() {
		return process(compositeFits, documents);
	}

	@Benchmark
	public long maxSize_cut() {
		return process(maxSizeCut, documents);
	}

	@Benchmark
	public long composite_cut() {
		return process(compositeCut, documents);
	}
}
