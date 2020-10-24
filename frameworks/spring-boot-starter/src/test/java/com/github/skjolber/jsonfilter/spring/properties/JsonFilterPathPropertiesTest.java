package com.github.skjolber.jsonfilter.spring.properties;

import static org.junit.Assert.*;

import org.junit.jupiter.api.Test;

public class JsonFilterPathPropertiesTest {

	@Test
	public void testDefaultCanBeChanged() {
		JsonFilterPathProperties properties = new JsonFilterPathProperties();
		assertTrue(properties.isEnabled());
		
		properties.setEnabled(false);
		assertFalse(properties.isEnabled());
	}
}
