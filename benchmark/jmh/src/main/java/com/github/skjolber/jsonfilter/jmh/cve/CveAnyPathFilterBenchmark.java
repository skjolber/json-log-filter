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

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.jmh.AbstractAnyPathFilterBenchmark;
import com.github.skjolber.jsonfilter.jmh.AbstractSingleFullPathMaxStringLengthFilterBenchmark;


@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)

@Fork(1)
public class CveAnyPathFilterBenchmark extends AbstractAnyPathFilterBenchmark {

	private static String PATH = "//vendor_name";

	@Param(value={"2KB","8KB","14KB","22KB","30KB","50KB","70KB","100KB","200KB"})
	//@Param(value={"2KB"})
	private String fileName;

	@Override
	protected File getFile() {
		return new File("./src/test/resources/benchmark/cves/" + fileName);
	}

	@Override
	protected FilterType getFilterType() {
		return FilterType.ANON;
	}

	@Override
	protected String getPath() {
		return PATH;
	}
	
	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(CveAnyPathFilterBenchmark.class.getSimpleName())
				.warmupIterations(5)
				.measurementIterations(2)
				.result("target/" + System.currentTimeMillis() + "." + CveAnyPathFilterBenchmark.class.getSimpleName() + ".json")
				.resultFormat(ResultFormatType.JSON)
				.build();

		new Runner(opt).run();
	}

}