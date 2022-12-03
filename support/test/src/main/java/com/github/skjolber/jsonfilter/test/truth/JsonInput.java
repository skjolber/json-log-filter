package com.github.skjolber.jsonfilter.test.truth;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public final class JsonInput {
	
	private final Path source;
	
	private final String contentAsString;
	
	private final List<String> prettyPrinted;

	public JsonInput(Path source, String content, List<String> prettyPrinted) {
		this.source = source;
		this.contentAsString = content;
		this.prettyPrinted = prettyPrinted;
	}

	public String getContentAsString() {
		return contentAsString;
	}

	public byte[] getContentAsBytes() {
		return contentAsString.getBytes(StandardCharsets.UTF_8);
	}

	public int getPrettyPrintedSize() {
		return prettyPrinted.size();
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
	
}