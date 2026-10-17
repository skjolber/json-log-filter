package com.github.skjolber.jsonfilter.jmh.entur;

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
 * The String API on the Entur documents, which have non-ASCII characters.
 */

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class EnturProcessStringFilterBenchmark extends AbstractProcessStringFilterBenchmark {

	@Param(value={"6KB","20KB","200KB"})
	private String fileName;

	@Override
	protected int getMaxStringLength() {
		// the longest strings are about 50 characters
		return 32;
	}

	@Override
	protected File getDirectory() {
		return new File("./src/test/resources/benchmark/entur/" + fileName);
	}
}
