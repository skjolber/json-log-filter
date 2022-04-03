package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.github.skjolber.jsonfilter.test.Generator;

public class MaxSizeJsonFilterTest {

	private JsonFactory factory = new JsonFactory();

	@Test
	public void testMaxSizeChars() throws IOException {
		String string = IOUtils.toString(getClass().getResourceAsStream("/json/maxSize/cve2006.json.gz.json"), StandardCharsets.UTF_8);

		for(int i = 2; i < string.length(); i++) {		
			MaxSizeJsonFilter filter = new MaxSizeJsonFilter(i);
		
			String process = filter.process(string);
	
			assertTrue(process.length() < i + 16);

			validate(process);
		}
	}

	@Test
	public void testMaxSizeBytes() throws IOException {
		byte[] string = IOUtils.toByteArray(getClass().getResourceAsStream("/json/maxSize/cve2006.json.gz.json"));

		for(int i = 2; i < string.length; i++) {		
			MaxSizeJsonFilter filter = new MaxSizeJsonFilter(i);
		
			byte[] process = filter.process(string);
	
			assertTrue(process.length < i + 16);

			validate(process);
		}
	}

	@Test
	public void testDeepStructureBytes() throws IOException {
		int levels = 100;
		byte[] generateDeepStructure = Generator.generateDeepStructure(levels);

		for(int i = 2; i < generateDeepStructure.length; i++) {		
			MaxSizeJsonFilter filter = new MaxSizeJsonFilter(i);
		
			byte[] process = filter.process(generateDeepStructure);
	
			assertTrue(process.length <= i + levels);

			validate(process);
		}
	}

	@Test
	public void testDeepStructureChars() throws IOException {
		int levels = 100;
		String string = new String(Generator.generateDeepStructure(levels), StandardCharsets.UTF_8);

		for(int i = 2; i < string.length(); i++) {		
			MaxSizeJsonFilter filter = new MaxSizeJsonFilter(i);
		
			String process = filter.process(string);
	
			assertTrue(process.length() <= i + levels);

			validate(process);
		}
	}

	@Test
	public void testInvalidInput() throws Exception {
		String string = new String(Generator.generateDeepStructure(1000), StandardCharsets.UTF_8);

		String broken = string.substring(0, string.length() / 2);
		
		MaxSizeJsonFilter filter = new MaxSizeJsonFilter(string.length());

		char[] brokenChars = broken.toCharArray();
		assertFalse(filter.process(brokenChars, 0, string.length(), new StringBuilder()));
		
		byte[] brokenBytes = broken.getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(brokenBytes, 0, string.length(), new ByteArrayOutputStream()));
		
		filter = new MaxSizeJsonFilter(brokenBytes.length);

		assertFalse(filter.process(new char[]{}, 0, string.length(), new StringBuilder()));
		
		assertFalse(filter.process(new byte[]{}, 0, string.length(), new ByteArrayOutputStream()));
	}

	@Test
	public void testMaxSizeFilteringNotNecessaryBytes() throws IOException {
		int levels = 100;
		byte[] generateDeepStructure = Generator.generateDeepStructure(levels);

		for(int i = 2; i < generateDeepStructure.length; i++) {		
			MaxSizeJsonFilter filter = new MaxSizeJsonFilter(generateDeepStructure.length);
		
			byte[] process = filter.process(generateDeepStructure);
	
			assertEquals(process.length, generateDeepStructure.length);

			validate(process);
		}
	}

	@Test
	public void testMaxSizeFilteringNotNecessaryChars() throws IOException {
		int levels = 100;
		String string = new String(Generator.generateDeepStructure(levels), StandardCharsets.UTF_8);

		for(int i = 2; i < string.length(); i++) {		
			MaxSizeJsonFilter filter = new MaxSizeJsonFilter(string.length());
		
			String process = filter.process(string);
	
			assertEquals(process.length(), string.length());

			validate(process);
		}
	}

	private void validate(byte[] process) throws IOException, JsonParseException {
		try (JsonParser parse = factory.createParser(process)) {
			while(parse.nextToken() != null);
		}
	}

	private void validate(String process) throws IOException, JsonParseException {
		try (JsonParser parse = factory.createParser(process)) {
			while(parse.nextToken() != null);
		}
	}

}
