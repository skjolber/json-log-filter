package com.github.skjolber.jsonfilter.jackson;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;

/**
 * A document which fits within max size can still grow beyond it when prune or anonymize
 * messages are longer than the values they replace.
 */

public class JacksonMaxSizeReplacementGrowthTest {

	private static final String INPUT = "[{\"id\":0},{\"id\":0},{\"id\":0},{\"id\":0},{\"id\":0}]";

	@Test
	public void outputWithinMaxSize() {
		for(int maxSize = INPUT.length() / 2; maxSize <= INPUT.length(); maxSize++) {
			JsonFilter filter = JacksonJsonLogFilterBuilder.createInstance().withPruneKeys("id").withMaxSize(maxSize).build();
			String name = filter.getClass().getSimpleName() + " " + maxSize;

			String chars = filter.process(INPUT);
			assertNotNull(chars, name);
			assertTrue(chars.length() <= maxSize, name + ": " + chars);

			byte[] bytes = filter.process(INPUT.getBytes(StandardCharsets.UTF_8));
			assertNotNull(bytes, name);
			assertTrue(bytes.length <= maxSize, name + ": " + new String(bytes, StandardCharsets.UTF_8));
		}
	}
}
