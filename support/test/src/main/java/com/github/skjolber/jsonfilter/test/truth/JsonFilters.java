package com.github.skjolber.jsonfilter.test.truth;

import com.github.skjolber.jsonfilter.JsonFilter;

/**
 * 
 * Wrapper for filters, to better handle differences in char and byte processing.
 * 
 */

public class JsonFilters {
	
	protected JsonFilter characters;
	protected JsonFilter bytes;
	
	public JsonFilters(JsonFilter filter) {
		this.characters = this.bytes = filter;
	}

	public JsonFilters(JsonFilter characters, JsonFilter bytes) {
		super();
		this.characters = characters;
		this.bytes = bytes;
	}
	
	public JsonFilter getCharacters() {
		return characters;
	}
	
	public JsonFilter getBytes() {
		return bytes;
	}
	
}