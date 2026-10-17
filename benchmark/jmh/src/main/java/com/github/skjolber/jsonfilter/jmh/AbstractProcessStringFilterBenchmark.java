package com.github.skjolber.jsonfilter.jmh;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.AnyPathJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxStringLengthJsonFilter;

/**
 * The String API, process(String), with a filter which changes every document and with filters which leave the
 * documents as they are, as with most log messages.
 */

public abstract class AbstractProcessStringFilterBenchmark {

	private String[] documents;

	private JsonFilter truncate;
	private JsonFilter maxStringLengthUnchanged;
	private JsonFilter anyPathUnchanged;

	@Setup
	public void init() throws Exception {
		File[] files = getDirectory().listFiles();
		Arrays.sort(files);
		documents = new String[files.length];
		for(int i = 0; i < files.length; i++) {
			documents[i] = new String(Files.readAllBytes(files[i].toPath()), StandardCharsets.UTF_8);
		}

		truncate = new MaxStringLengthJsonFilter(getMaxStringLength());
		maxStringLengthUnchanged = new MaxStringLengthJsonFilter(64 * 1024);
		anyPathUnchanged = new AnyPathJsonFilter(-1, new String[] {"//notInTheDocuments"}, null);

		for(String document : documents) {
			if(truncate.process(document).equals(document)) {
				throw new IllegalStateException("Expected a change");
			}
			if(!maxStringLengthUnchanged.process(document).equals(document) || !anyPathUnchanged.process(document).equals(document)) {
				throw new IllegalStateException("Expected no change");
			}
		}
	}

	protected abstract File getDirectory();

	/**
	 * @return max string length which changes every document
	 */

	protected int getMaxStringLength() {
		return 64;
	}

	private static long process(JsonFilter filter, String[] documents) {
		long sizeSum = 0;
		for(String document : documents) {
			sizeSum += filter.process(document).length();
		}
		return sizeSum;
	}

	@Benchmark
	public long maxStringLength_changed() {
		return process(truncate, documents);
	}

	@Benchmark
	public long maxStringLength_unchanged() {
		return process(maxStringLengthUnchanged, documents);
	}

	@Benchmark
	public long anyPath_unchanged() {
		return process(anyPathUnchanged, documents);
	}
}
