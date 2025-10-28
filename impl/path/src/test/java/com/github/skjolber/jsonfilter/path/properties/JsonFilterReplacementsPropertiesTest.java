package com.github.skjolber.jsonfilter.path.properties;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

public class JsonFilterReplacementsPropertiesTest {

	@Test
	public void test() {
		JsonFilterReplacementsProperties properties = new JsonFilterReplacementsProperties();
		
		assertFalse(properties.hasPrune());
		properties.setPrune("*");
		assertTrue(properties.hasPrune());

		assertFalse(properties.hasAnonymize());
		properties.setAnonymize("**");
		assertTrue(properties.hasAnonymize());

		assertFalse(properties.hasTruncate());
		properties.setTruncate("XXX");
		assertTrue(properties.hasTruncate());
	}
}
