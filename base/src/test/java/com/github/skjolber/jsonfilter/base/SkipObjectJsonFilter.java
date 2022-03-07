package com.github.skjolber.jsonfilter.base;

import java.io.ByteArrayOutputStream;

public class SkipObjectJsonFilter extends AbstractJsonFilter {

	public SkipObjectJsonFilter(int maxStringLength, String pruneJson, String anonymizeJson,
			String truncateJsonString) {
		super(maxStringLength, pruneJson, anonymizeJson, truncateJsonString, -1);
	}

	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		output.append(chars, offset, length);
		if(chars[offset] != '{') {
			return true;
		}
		return CharArrayRangesFilter.skipObject(chars, offset) == offset + length;
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
		output.write(chars, offset, length);
		if(chars[offset] != '{') {
			return true;
		}
		return ByteArrayRangesFilter.skipObject(chars, offset) == offset + length;
	}

}
