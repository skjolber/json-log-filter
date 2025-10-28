package com.github.skjolber.jsonfilter.path.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

public class ProcessingPropertiesTest {

	@Test
	public void test() {
		ProcessingProperties properties = new ProcessingProperties();
		
		assertFalse(properties.hasMaxSize());
		properties.setMaxSize(1);
		assertTrue(properties.hasMaxSize());
		assertEquals(1, properties.getMaxSize());
		
		assertFalse(properties.hasWhitespaceStrategy());
		properties.setWhitespaceStrategy(WhitespaceStrategy.NEVER);
		assertTrue(properties.hasWhitespaceStrategy());

		assertFalse(properties.isValidate());
		properties.setValidate(true);
		assertTrue(properties.isValidate());
	}
}
