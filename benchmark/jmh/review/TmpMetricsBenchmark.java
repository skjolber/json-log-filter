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
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilterMetrics;
import com.github.skjolber.jsonfilter.core.AnyPathJsonFilter;
import com.github.skjolber.jsonfilter.core.DefaultJsonLogFilterBuilder;
import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.PathMaxSizeMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonPathMaxSizeMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jmh.BenchmarkRunner;
import com.github.skjolber.jsonfilter.jmh.fileutils.FileDirectoryValue;

/**
 * Not part of the benchmark module: campaign.sh copies it into the builds it compares. Measures the cost of
 * metrics: each filter with metrics off ({@code _off}) and on ({@code _on}). No other benchmark passes metrics.
 * Max size is half the smallest document, so the max-size filters cut every document.
 */

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class TmpMetricsBenchmark {

	private static final int MAX_STRING_LENGTH = 64;
	private static final String[] ANON = new String[] {"/CVE_Items/cve/affects/vendor/vendor_data/vendor_name"};
	private static final String[] PRUNE = new String[] {"/CVE_Items/cve/references", "//version"};

	@Param(value={"2KB", "22KB", "200KB"})
	private String fileName;

	private Runner anyPath;
	private Runner path;
	private Runner maxStringLength;
	private Runner maxSize;
	private Runner pathMaxSize;
	private Runner maxSizeRemoveWhitespace;
	private Runner pathMaxSizeRemoveWhitespace;
	private Runner jacksonPathMaxSize;

	private final DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();

	@Setup
	public void init() throws IOException {
		File file = new File("./src/test/resources/benchmark/cves/" + fileName);
		long minimum = Long.MAX_VALUE;
		for (File f : file.listFiles()) {
			minimum = Math.min(minimum, f.length());
		}
		int maxSize = (int)(minimum / 2);

		anyPath = new Runner(file, new AnyPathJsonFilter(-1, new String[] {"//version"}, null));
		path = new Runner(file, new DefaultJsonLogFilterBuilder().withMaxStringLength(MAX_STRING_LENGTH).withAnonymize(ANON).withPrune(PRUNE).build());
		maxStringLength = new Runner(file, new MaxStringLengthJsonFilter(MAX_STRING_LENGTH));
		this.maxSize = new Runner(file, new MaxSizeJsonFilter(maxSize));
		pathMaxSize = new Runner(file, new PathMaxSizeMaxStringLengthJsonFilter(MAX_STRING_LENGTH, maxSize, -1, ANON, PRUNE));
		maxSizeRemoveWhitespace = new Runner(file, new MaxSizeRemoveWhitespaceJsonFilter(maxSize));
		pathMaxSizeRemoveWhitespace = new Runner(file, new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(MAX_STRING_LENGTH, maxSize, -1, ANON, PRUNE));
		jacksonPathMaxSize = new Runner(file, new JacksonPathMaxSizeMaxStringLengthJsonFilter(MAX_STRING_LENGTH, maxSize, ANON, PRUNE));
	}

	private static class Runner extends BenchmarkRunner<JsonFilter> {

		Runner(File file, JsonFilter filter) throws IOException {
			super(file, true, filter, false);
		}

		long run(JsonFilterMetrics metrics) {
			long sizeSum = 0;
			for(FileDirectoryValue directory : directories) {
				for(int i = 0; i < directory.size(); i++) {
					byte[] bytes = directory.getValueAsBytes(i);
					if(!jsonFilter.process(bytes, 0, bytes.length, outputstream, metrics)) {
						throw new RuntimeException("Unable to filter using " + jsonFilter + " for source " + directory.getFile(i));
					}
					sizeSum += outputstream.size();
					outputstream.reset();
				}
			}
			return sizeSum;
		}
	}

	@Benchmark
	public long anyPath_off() {
		return anyPath.run(null);
	}

	@Benchmark
	public long anyPath_on() {
		return anyPath.run(metrics);
	}

	@Benchmark
	public long path_off() {
		return path.run(null);
	}

	@Benchmark
	public long path_on() {
		return path.run(metrics);
	}

	@Benchmark
	public long maxStringLength_off() {
		return maxStringLength.run(null);
	}

	@Benchmark
	public long maxStringLength_on() {
		return maxStringLength.run(metrics);
	}

	@Benchmark
	public long maxSize_off() {
		return maxSize.run(null);
	}

	@Benchmark
	public long maxSize_on() {
		return maxSize.run(metrics);
	}

	@Benchmark
	public long pathMaxSize_off() {
		return pathMaxSize.run(null);
	}

	@Benchmark
	public long pathMaxSize_on() {
		return pathMaxSize.run(metrics);
	}

	@Benchmark
	public long maxSizeRemoveWhitespace_off() {
		return maxSizeRemoveWhitespace.run(null);
	}

	@Benchmark
	public long maxSizeRemoveWhitespace_on() {
		return maxSizeRemoveWhitespace.run(metrics);
	}

	@Benchmark
	public long pathMaxSizeRemoveWhitespace_off() {
		return pathMaxSizeRemoveWhitespace.run(null);
	}

	@Benchmark
	public long pathMaxSizeRemoveWhitespace_on() {
		return pathMaxSizeRemoveWhitespace.run(metrics);
	}

	@Benchmark
	public long jacksonPathMaxSize_off() {
		return jacksonPathMaxSize.run(null);
	}

	@Benchmark
	public long jacksonPathMaxSize_on() {
		return jacksonPathMaxSize.run(metrics);
	}
}
