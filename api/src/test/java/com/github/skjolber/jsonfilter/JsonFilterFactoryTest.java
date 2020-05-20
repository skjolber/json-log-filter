package com.github.skjolber.jsonfilter;

import static org.junit.Assert.assertNull;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilterFactory.JsonFilterFactoryProperty;
import static com.google.common.truth.Truth.*;

public class JsonFilterFactoryTest {

	@Test
	public void testParse() {
		for (JsonFilterFactoryProperty p : JsonFilterFactory.JsonFilterFactoryProperty.values()) {
			assertThat(p).isEqualTo(JsonFilterFactory.JsonFilterFactoryProperty.parse(p.getPropertyName()));
		}
		assertNull(JsonFilterFactory.JsonFilterFactoryProperty.parse("abc"));
	}
}
