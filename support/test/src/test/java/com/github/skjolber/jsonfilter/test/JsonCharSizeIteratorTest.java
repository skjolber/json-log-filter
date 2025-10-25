package com.github.skjolber.jsonfilter.test;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonItem;
import com.github.skjolber.jsonfilter.test.jackson.JsonCharSizeIterator;

public class JsonCharSizeIteratorTest {

	@Test
	public void testIterate() throws Exception {
		String json = IOUtils.resourceToString("/person.json", StandardCharsets.UTF_8);

		JsonCharSizeIterator iterator = new JsonCharSizeIterator(json);
		
		while(iterator.hasNext()) {
			MaxSizeJsonItem next = iterator.next();
			String value = next.getContentAsString();
			System.out.println(value);
		}
		
	}
	
}

