package com.github.skjolber.jsonfilter.test.cache;

import java.nio.file.Path;
import java.util.List;

public class JsonFile extends JsonPermutation {
	
	private final Path source;
	private final List<MaxSizeJsonCollection> maxSizePermutations;
	
	public JsonFile(Path source, String content, List<String> prettyPrinted, List<MaxSizeJsonCollection> maxSizePermutations) {
		super(content, prettyPrinted);
		this.source = source;
		this.maxSizePermutations = maxSizePermutations;
	}
	
	public Path getSource() {
		return source;
	}
	
	public List<MaxSizeJsonCollection> getMaxSizeCollections() {
		return maxSizePermutations;
	}
	
	public int getMaxPrettyPrintedContentLength() {
		MaxSizeJsonCollection maxSizeJsonPermutation = maxSizePermutations.get(maxSizePermutations.size() - 1);
		
		return maxSizeJsonPermutation.getMaxLength();
	}

	public int getContentLength() {
		return contentAsString.length();
	}

	public boolean hasPrettyPrinted() {
		return !prettyPrinted.isEmpty();
	}
	
}