package com.github.skjolber.jsonfilter.base;

import java.io.ByteArrayOutputStream;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;

public class SkipSubtreeJsonFilter extends AbstractJsonFilter {

	public SkipSubtreeJsonFilter(int maxStringLength, String pruneJson, String anonymizeJson,
			String truncateJsonString) {
		super(maxStringLength, -1, pruneJson, anonymizeJson, truncateJsonString);
	}

	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		output.append(chars, offset, length);
		return CharArrayRangesFilter.skipSubtree(chars, offset) == offset + length;
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
		output.write(chars, offset, length);
		return ByteArrayRangesFilter.skipSubtree(chars, offset) == offset + length;
	}
	

	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output,
			JsonFilterMetrics filterMetrics) {
		return process(chars, offset, length, output);
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output,
			JsonFilterMetrics filterMetrics) {
		return process(chars, offset, length, output);
	}

}
