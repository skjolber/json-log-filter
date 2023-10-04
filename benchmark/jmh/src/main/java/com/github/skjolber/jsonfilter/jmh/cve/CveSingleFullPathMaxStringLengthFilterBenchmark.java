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
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.json.JsonPathBodyFilters;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.core.DefaultJsonLogFilterBuilder;
import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxStringLengthMaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.MultiPathMaxSizeMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.SingleFullPathMaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.SingleFullPathMaxSizeMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxStringLengthRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.RemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonJsonLogFilterBuilder;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxSizeMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMultiPathMaxSizeMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jmh.AbstractMaxSizeFilterBenchmark;
import com.github.skjolber.jsonfilter.jmh.AbstractMaxStringLengthFilterBenchmark;
import com.github.skjolber.jsonfilter.jmh.AbstractSingleFullPathMaxStringLengthFilterBenchmark;


@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)

@Fork(1)
public class CveSingleFullPathMaxStringLengthFilterBenchmark extends AbstractSingleFullPathMaxStringLengthFilterBenchmark {

	private static String PATH = "/CVE_Items/cve/affects/vendor/vendor_data/vendor_name";

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
	protected FilterType getFilterType() {
		return FilterType.ANON;
	}

	@Override
	protected String getPath() {
		return PATH;
	}
	
	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(CveSingleFullPathMaxStringLengthFilterBenchmark.class.getSimpleName())
				.warmupIterations(5)
				.measurementIterations(2)
				.result("target/" + System.currentTimeMillis() + "." + CveSingleFullPathMaxStringLengthFilterBenchmark.class.getSimpleName() + ".json")
				.resultFormat(ResultFormatType.JSON)
				.build();

		new Runner(opt).run();
	}

}