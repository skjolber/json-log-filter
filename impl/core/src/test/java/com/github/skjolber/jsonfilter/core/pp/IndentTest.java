package com.github.skjolber.jsonfilter.core.pp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IndentTest {
	
	@Test
	public void testConstructors() {
		Assertions.assertThrows(
			IllegalArgumentException.class,
			() -> {
				new Indent(' ', -1, 0, LinebreakType.CarriageReturn);
			}
		);
		Assertions.assertThrows(
				IllegalArgumentException.class,
				() -> {
					new Indent(' ', 0, -1, LinebreakType.CarriageReturn);
				}
			);
		Assertions.assertThrows(
				IllegalArgumentException.class,
				() -> {
					new Indent(' ', 0, 0, null);
				}
			);
	}

	@Test
	public void testSpaces() {
		Indent build = Indent.newBuilder().withSpace(2).withUnixLinebreak().build();
		assertEquals(build.asIndent(), "  ");
	}
	
	@Test
	public void testTab() {
		Indent build = Indent.newBuilder().withTab().withUnixLinebreak().build();
		assertEquals(build.asIndent(), "\t");
	}

	@Test
	public void testWindows() {
		Indent build = Indent.newBuilder().withTab().withWindowsLinebreak().build();
		assertEquals(build.asLinebreak(), "\r\n");
	}

	@Test
	public void testPreparedLevels() {
		Indent build = Indent.newBuilder().withSpace(2).withUnixLinebreak().withPreparedLevels(64).build();
		assertEquals(build.asIndent(), "  ");
	}
}
