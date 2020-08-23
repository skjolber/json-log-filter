package com.github.skjolber.jsonfilter.spring.logbook;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;

public class JsonBodyFilterTest {

	@Test
	public void testJson() {
		JsonFilter jsonFilter = new DefaultJsonFilter();
		JsonBodyFilter jsonBodyFilter = new JsonBodyFilter(jsonFilter);
		
		assertEquals(jsonBodyFilter.filter("appliction/json", "{}"), "{}");
		assertEquals(jsonBodyFilter.filter("appliction/xml", "{}"), "{}");
		assertEquals(jsonBodyFilter.filter(null, "{}"), "{}");
	}
}
