package com.github.skjolber.jsonfilter.test.truth;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import com.github.skjolber.jsonfilter.test.jackson.JsonNormalizer;

public final class JsonInput {
	
	private final Path source;
	
	private final String contentAsString;
	
	private final List<String> prettyPrinted;

	public JsonInput(Path source, String content, List<String> prettyPrinted) {
		this.source = source;
		this.contentAsString = content;
		this.prettyPrinted = prettyPrinted;
	}

	public int getContentAsStringSize() {
		return contentAsString.length();
	}

	public String getContentAsString(int length) {
		if(length > contentAsString.length()) {
			return padded(contentAsString, length);
		}
		return contentAsString;
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
	
	public Path getSource() {
		return source;
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