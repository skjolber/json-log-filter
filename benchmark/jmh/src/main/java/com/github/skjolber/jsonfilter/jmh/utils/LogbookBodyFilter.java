package com.github.skjolber.jsonfilter.jmh.utils;

import org.zalando.logbook.BodyFilter;

import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;

public class LogbookBodyFilter extends DefaultJsonFilter {

	private BodyFilter bodyFilter;
	
	public LogbookBodyFilter(BodyFilter bodyFilter) {
		this.bodyFilter = bodyFilter;
	}

	@Override
	public String process(String chars) {
		return bodyFilter.filter("application/json", chars);
	}
	
	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer) {
		buffer.append(bodyFilter.filter("application/json", new String(chars, offset, length)));

		return true;
	}
	
	
}
