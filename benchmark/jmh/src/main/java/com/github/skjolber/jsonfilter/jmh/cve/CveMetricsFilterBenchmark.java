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
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.core.MaxStringLengthJsonFilter;

/**
 * The String and byte[] APIs with metrics, with a filter which changes every document and with one which leaves
 * the documents as they are.
 */

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class CveMetricsFilterBenchmark {

	private static class CountingMetrics implements JsonFilterMetrics {

		private long count;

		@Override
		public void onMaxStringLength(int count) {
			this.count += count;
		}

		@Override
		public void onMaxSize(int count) {
			this.count += count;
		}

		@Override
		public void onPrune(int count) {
			this.count += count;
		}

		@Override
		public void onAnonymize(int count) {
			this.count += count;
		}

		@Override
		public void onInput(int inputSize) {
			this.count += inputSize;
		}

		@Override
		public void onOutput(int outputSize) {
			this.count += outputSize;
		}
	}

	@Param(value={"2KB","22KB","200KB"})
	private String fileName;

	private String[] documents;
	private byte[][] documentsAsBytes;

	private JsonFilter truncate;
	private JsonFilter unchanged;

	private final CountingMetrics metrics = new CountingMetrics();

	@Setup
	public void init() throws Exception {
		File[] files = new File("./src/test/resources/benchmark/cves/" + fileName).listFiles();
		Arrays.sort(files);
		documents = new String[files.length];
		documentsAsBytes = new byte[files.length][];
		for(int i = 0; i < files.length; i++) {
			documentsAsBytes[i] = Files.readAllBytes(files[i].toPath());
			documents[i] = new String(documentsAsBytes[i], StandardCharsets.UTF_8);
		}

		truncate = new MaxStringLengthJsonFilter(64);
		unchanged = new MaxStringLengthJsonFilter(64 * 1024);

		for(String document : documents) {
			if(truncate.process(document, metrics).equals(document) || !unchanged.process(document, metrics).equals(document)) {
				throw new IllegalStateException();
			}
		}
	}

	private long string(JsonFilter filter) {
		long sizeSum = 0;
		for(String document : documents) {
			sizeSum += filter.process(document, metrics).length();
		}
		return sizeSum + metrics.count;
	}

	private long bytes(JsonFilter filter) {
		long sizeSum = 0;
		for(byte[] document : documentsAsBytes) {
			sizeSum += filter.process(document, metrics).length;
		}
		return sizeSum + metrics.count;
	}

	@Benchmark
	public long string_changed() {
		return string(truncate);
	}

	@Benchmark
	public long string_unchanged() {
		return string(unchanged);
	}

	@Benchmark
	public long bytes_changed() {
		return bytes(truncate);
	}

	@Benchmark
	public long bytes_unchanged() {
		return bytes(unchanged);
	}
}
