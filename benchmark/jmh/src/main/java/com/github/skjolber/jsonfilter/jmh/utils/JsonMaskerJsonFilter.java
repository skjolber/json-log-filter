package com.github.skjolber.jsonfilter.jmh.utils;

import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;

import dev.blaauwendraad.masker.json.JsonMasker;

public class JsonMaskerJsonFilter extends DefaultJsonFilter {
	
	private final JsonMasker masker;
	
	public JsonMaskerJsonFilter(JsonMasker masker) {
		this.masker = masker;
	}
	
	@Override
	public byte[] process(byte[] chars) {
		return masker.mask(chars);
	}
	
	@Override
	public String process(String chars) {
		return masker.mask(chars);
	}

}
