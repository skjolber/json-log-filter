package com.github.skjolber.jsonfilter.core.pp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class IndentTest {

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

}
