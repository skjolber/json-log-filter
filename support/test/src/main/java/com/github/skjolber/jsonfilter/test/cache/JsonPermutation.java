package com.github.skjolber.jsonfilter.test.cache;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.github.skjolber.jsonfilter.test.jackson.JsonNormalizer;

public class JsonPermutation {
	
	protected final String contentAsString;
	protected final List<String> prettyPrinted;

	public JsonPermutation(String content, List<String> prettyPrinted) {
		this.contentAsString = content;
		this.prettyPrinted = prettyPrinted;
	}

	public int getContentAsStringSize() {
		return contentAsString.length();
	}
	
	public int getContentAsBytesSize() {
		return getContentAsBytes().length;
	}

	public String getContentAsString(int length) {
		if(length > contentAsString.length()) {
			return padded(contentAsString, length);
		}
		return contentAsString;
	}
	
	public String getContentAsString() {
		return contentAsString;
	}

	public byte[] getContentAsBytes() {
		return contentAsString.getBytes(StandardCharsets.UTF_8);
	}

	private String padded(String content, int length) {
		StringBuilder builder = new StringBuilder(content.length() + length);
		builder.append(content);
		for(int i = 0; i < length; i++) {
			builder.append(' ');
		}
		return builder.toString();
	}

	public byte[] getContentAsBytes(int length) {
		byte[] bytes = contentAsString.getBytes(StandardCharsets.UTF_8);
		if(length > bytes.length) {
			return padded(bytes, length);
		}
		return bytes;
	}

	public int getPrettyPrintedSize() {
		return prettyPrinted.size();
	}
	
	public String getPrettyPrintedAsString(int index, int length) {
		String prettyPrintedContentAsString = prettyPrinted.get(index);
		if(length > prettyPrintedContentAsString.length()) {
			return padded(prettyPrintedContentAsString, length);
		}
		return prettyPrintedContentAsString;
	}
	
	public byte[] getPrettyPrintedAsBytes(int index, int length) {
		byte[] prettyPrintedAsBytes = prettyPrinted.get(index).getBytes(StandardCharsets.UTF_8);
		if(length > prettyPrintedAsBytes.length) {
			return padded(prettyPrintedAsBytes, length);
		}
		return prettyPrintedAsBytes;
	}

	public String getPrettyPrintedAsString(int index) {
		return prettyPrinted.get(index);
	}

	public byte[] getPrettyPrintedAsBytes(int index) {
		return prettyPrinted.get(index).getBytes(StandardCharsets.UTF_8);
	}
	
	private byte[] padded(byte[] content, int length) {
		byte[] c = new byte[content.length + length];
		
		System.arraycopy(content, 0, c, 0, content.length);
		for(int i = 0; i < length; i++) {
			c[content.length + i] = ' ';
		}
		
		return c;
	}

	public boolean hasUnicode() {
		return JsonNormalizer.isHighSurrogate(contentAsString);
	}

	public boolean hasEscapeSequence() {
		return JsonNormalizer.isEscape(contentAsString);
	}
}