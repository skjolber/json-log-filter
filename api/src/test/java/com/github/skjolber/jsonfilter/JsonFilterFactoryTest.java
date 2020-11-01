package com.github.skjolber.jsonfilter;

import static org.junit.Assert.assertNull;

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.*;

public class JsonFilterFactoryTest {

	@Test
	public void testParse() {
		for (JsonFilterFactoryProperty p : JsonFilterFactoryProperty.values()) {
			assertThat(p).isEqualTo(JsonFilterFactoryProperty.parse(p.getPropertyName()));
		}
		assertNull(JsonFilterFactoryProperty.parse("abc"));
	}
}
