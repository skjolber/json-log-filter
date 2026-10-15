package com.github.skjolber.jsonfilter.core.pp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

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

	@Test
	public void testAppendWithZeroPreparedLevels() {
		// without prepared levels, each level is appended one indent at a time
		assertAppend(Indent.newBuilder().withSpace(2).withUnixLinebreak().withPreparedLevels(0).build(), "\n", "  ");
		assertAppend(Indent.newBuilder().withTab().withWindowsLinebreak().withPreparedLevels(0).build(), "\r\n", "\t");
		// non-ascii indent character, multiple bytes in UTF-8
		assertAppend(Indent.newBuilder().withCharacter(' ').withCount(3).withPreparedLevels(0).build(), "\n", "   ");
	}

	@Test
	public void testAppendBeyondPreparedLevels() {
		assertAppend(Indent.newBuilder().withSpace(2).withUnixLinebreak().withPreparedLevels(1).build(), "\n", "  ");
		assertAppend(Indent.newBuilder().withSpace(2).withUnixLinebreak().withPreparedLevels(3).build(), "\n", "  ");
	}

	private static void assertAppend(Indent indent, String linebreak, String increment) {
		for(int level = 0; level < 10; level++) {
			String expected = linebreak + increment.repeat(level);

			StringBuilder chars = new StringBuilder();
			indent.append(chars, level);
			assertEquals(expected, chars.toString());

			ResizableByteArrayOutputStream bytes = new ResizableByteArrayOutputStream(128);
			indent.append(bytes, level);
			assertEquals(expected, bytes.toString(StandardCharsets.UTF_8));
		}
	}
}
