package com.github.skjolber.jsonfilter.path.properties;

import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

public class JsonFilterReplacementsPropertiesTest {

	@Test
	public void test() {
		JsonFilterReplacementsProperties properties = new JsonFilterReplacementsProperties();
		
		properties.setPrune("*");
		assertTrue(properties.hasPrune());

		properties.setAnonymize("**");
		assertTrue(properties.hasAnonymize());

		properties.setTruncate("XXX");
		assertTrue(properties.hasTruncate());
	}
}
