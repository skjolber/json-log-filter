package com.github.skjolber.jsonfilter.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

public class PrettyPrinterIteratorTest {

	@Test
	public void testPrettyPrinterIterator() {
		List<DefaultPrettyPrinter> permutations = PrettyPrinterIterator.getPermutations();
		assertFalse(permutations.isEmpty());
	}
	
}
