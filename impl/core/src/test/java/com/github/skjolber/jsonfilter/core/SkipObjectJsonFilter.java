package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;

public class SkipObjectJsonFilter extends AbstractJsonFilter {

	public SkipObjectJsonFilter(int maxStringLength, String pruneJson, String anonymizeJson,
			String truncateJsonString) {
		super(maxStringLength, -1, pruneJson, anonymizeJson, truncateJsonString);
	}

	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		output.append(chars, offset, length);
		if(chars[offset] != '{') {
			return true;
		}
		return CharArrayRangesFilter.skipObject(chars, offset + 1) == offset + length;
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output) {
		output.write(chars, offset, length);
		if(chars[offset] != '{') {
			return true;
		}
		return ByteArrayRangesFilter.skipObject(chars, offset + 1) == offset + length;
	}

	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output,
			JsonFilterMetrics filterMetrics) {
		return process(chars, offset, length, output);
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output,
			JsonFilterMetrics filterMetrics) {
		return process(chars, offset, length, output);
	}

}
