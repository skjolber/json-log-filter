package com.github.skjolber.jsonfilter.spring.logbook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;

public class JsonBodyFilterTest {

	@Test
	public void testJson() {
		JsonFilter jsonFilter = mock(JsonFilter.class);
		when(jsonFilter.process("{}")).thenReturn("{} filtered");
		when(jsonFilter.process("{corrupt")).thenReturn(null);
		
		JsonBodyFilter jsonBodyFilter = new JsonBodyFilter(jsonFilter);
		
		assertEquals(jsonBodyFilter.filter("application/json", "{}"), "{} filtered");
		assertEquals(jsonBodyFilter.filter("application/xml", "{}"), "{}");
		assertEquals(jsonBodyFilter.filter("application/json", "{corrupt"), "{corrupt");
		assertEquals(jsonBodyFilter.filter(null, "{}"), "{}");
	}
}
