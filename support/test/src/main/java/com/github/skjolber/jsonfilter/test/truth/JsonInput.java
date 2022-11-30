package com.github.skjolber.jsonfilter.test.truth;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class JsonInput {
	
	private final File source;
	
	private final String contentAsString;
	
	private final List<String> prettyPrinted;

	public JsonInput(File file, String content, List<String> prettyPrinted) {
		this.source = file;
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

	public File getSource() {
		return source;
	}
	
}