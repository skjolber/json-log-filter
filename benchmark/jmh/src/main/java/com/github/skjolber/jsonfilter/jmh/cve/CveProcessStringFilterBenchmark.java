package com.github.skjolber.jsonfilter.jmh.cve;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import com.github.skjolber.jsonfilter.jmh.AbstractProcessStringFilterBenchmark;

/**
 * The String API on the CVE documents, which are ASCII.
 */

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class CveProcessStringFilterBenchmark extends AbstractProcessStringFilterBenchmark {

	@Param(value={"2KB","22KB","200KB"})
	private String fileName;

	@Override
	protected File getDirectory() {
		return new File("./src/test/resources/benchmark/cves/" + fileName);
	}
}
