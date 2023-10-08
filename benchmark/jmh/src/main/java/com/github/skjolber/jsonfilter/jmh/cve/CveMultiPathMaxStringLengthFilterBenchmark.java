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
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.github.skjolber.jsonfilter.jmh.AbstractMultiPathMaxStringLengthFilterBenchmark;


@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)

@Fork(1)
public class CveMultiPathMaxStringLengthFilterBenchmark extends AbstractMultiPathMaxStringLengthFilterBenchmark {

	private static String[] anon = new String[] {"/CVE_Items/cve/affects/vendor/vendor_data/vendor_name"};
	private static String[] prune = new String[] {"/CVE_Items/cve/references", "//version"};

	@Param(value={"2KB","8KB","14KB","22KB","30KB","50KB","70KB","100KB","200KB"})
	//@Param(value={"2KB"})
	private String fileName;

	@Override
	protected File getFile() {
		return new File("./src/test/resources/benchmark/cves/" + fileName);
	}
	
	@Override
	protected int getMaxStringLength() {
		return 64;
	}

	@Override
	protected String[] getPrune() {
		return prune;
	}

	@Override
	protected String[] getAnon() {
		return anon;
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(CveMultiPathMaxStringLengthFilterBenchmark.class.getSimpleName())
				.warmupIterations(5)
				.measurementIterations(2)
				.result("target/" + System.currentTimeMillis() + "." + CveMultiPathMaxStringLengthFilterBenchmark.class.getSimpleName() + ".json")
				.resultFormat(ResultFormatType.JSON)
				.build();

		new Runner(opt).run();
	}

}