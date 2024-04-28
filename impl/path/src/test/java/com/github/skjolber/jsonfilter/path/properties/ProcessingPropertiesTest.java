package com.github.skjolber.jsonfilter.path.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

public class ProcessingPropertiesTest {

	@Test
	public void test() {
		ProcessingProperties properties = new ProcessingProperties();
		
		properties.setMaxSize(1);
		assertTrue(properties.hasMaxSize());
		assertEquals(1, properties.getMaxSize());
		
		properties.setWhitespaceStrategy(WhitespaceStrategy.NEVER);
		assertTrue(properties.hasWhitespaceStrategy());

		properties.setValidate(true);
		assertTrue(properties.isValidate());
	}
}
