package com.github.skjolber.jsonfilter.test;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

public class PrettyPrinterIteratorTest {

	@Test
	public void testPrettyPrinterIterator() throws IOException {
		List<DefaultPrettyPrinter> permutations = PrettyPrinterIterator.getAll();
		assertFalse(permutations.isEmpty());
		String json = IOUtils.resourceToString("/person.json", StandardCharsets.UTF_8);
		Set<String> all = new HashSet<>();
		for (DefaultPrettyPrinter prettyPrinter : permutations) {
			PrettyPrintTransformer transformer = new PrettyPrintTransformer(prettyPrinter);
			String transformed = transformer.apply(json);
			all.add(transformed);
		}
		assertTrue(all.size() > 1);
	}
	
	@Test
	public void testEspace() {
		Appendable b = new StringBuilder();
		PrettyPrintTransformer.escape("acሴefghijklmnopqrst", b);
		assertTrue(b.toString().contains("\\u"));
	}
	
}
