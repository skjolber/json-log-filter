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
import com.github.skjolber.jsonfilter.core.CompositeMaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxSizeFromEndJsonFilter;
import com.github.skjolber.jsonfilter.jmh.BenchmarkRunner;

/**
 * The backward max-size scan, and the composite max-size filter which the default factory creates for max size only.
 * The max size is 3/4 of the smallest document, so the composite filter picks the backward scan for documents shorter
 * than 1.5 times the smallest.
 */

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class CveMaxSizeFromEndFilterBenchmark {

	@Param(value={"2KB","8KB","14KB","22KB","30KB","50KB","70KB","100KB","200KB"})
	private String fileName;

	private BenchmarkRunner<JsonFilter> fromEnd;
	private BenchmarkRunner<JsonFilter> composite;

	@Setup
	public void init() throws Exception {
		File file = new File("./src/test/resources/benchmark/cves/" + fileName);
		long minimum = Integer.MAX_VALUE;
		for (File f : file.listFiles()) {
			minimum = Math.min(minimum, f.length());
		}
		int maxSize = (int)(minimum * 3 / 4);
		fromEnd = new BenchmarkRunner<>(file, true, new MaxSizeFromEndJsonFilter(maxSize), false);
		composite = new BenchmarkRunner<>(file, true, new CompositeMaxSizeJsonFilter(maxSize), false);
	}

	@Benchmark
	public long from_end_bytes() throws IOException {
		return fromEnd.benchmarkBytes();
	}

	@Benchmark
	public long from_end_char() throws IOException {
		return fromEnd.benchmarkCharacters();
	}

	@Benchmark
	public long composite_bytes() throws IOException {
		return composite.benchmarkBytes();
	}

	@Benchmark
	public long composite_char() throws IOException {
		return composite.benchmarkCharacters();
	}
}
