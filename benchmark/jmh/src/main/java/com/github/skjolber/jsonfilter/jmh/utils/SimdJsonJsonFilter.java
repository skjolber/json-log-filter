package com.github.skjolber.jsonfilter.jmh.utils;

import java.util.Iterator;
import java.util.Map.Entry;

import org.simdjson.JsonValue;
import org.simdjson.SimdJsonParser;

import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;

import dev.blaauwendraad.masker.json.JsonMasker;

public class SimdJsonJsonFilter extends DefaultJsonFilter {
	
	private final SimdJsonParser parser;
	
	public SimdJsonJsonFilter(SimdJsonParser parser) {
		this.parser = parser;
	}
	
	@Override
	public byte[] process(byte[] chars) {
		JsonValue jsonValue = parser.parse(chars, chars.length);
		
		if(jsonValue.isObject()) {
			Iterator<Entry<CharSequence, JsonValue>> objectIterator = jsonValue.objectIterator();
			
		}
		
		Iterator<JsonValue> tweets = jsonValue.get("statuses").arrayIterator();
		while (tweets.hasNext()) {
		    JsonValue tweet = tweets.next();
		    JsonValue user = tweet.get("user");
		    if (user.get("default_profile").asBoolean()) {
		        System.out.println(user.get("screen_name").asString());
		    }
		}
		
		return null; // TODO masker.mask(chars);
	}
	
	@Override
	public String process(String chars) {
		//return masker.mask(chars);
		throw new RuntimeException();
	}
}
