package com.github.skjolber.jsonfilter.core.pp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class LinebreakTypeTest {

	@Test
	public void testParse() {
		assertEquals(LinebreakType.LineFeed, LinebreakType.parse("\n"));
	}

	@Test
	public void testUnknown() {
		assertThrows(IllegalArgumentException.class, () -> {
			LinebreakType.parse("N");
		});
	}
}
