package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class SingleFullPathMaxSizeMaxStringLengthJsonFilter2Test {

	private JsonFactory factory = new JsonFactory();

	@Test
	public void testMaxSizeChars() throws IOException {
		String string = IOUtils.toString(getClass().getResourceAsStream("/json/maxSize/cve2006.json.gz.json"), StandardCharsets.UTF_8);

		for(int i = 2; i < string.length(); i++) {		
			SingleFullPathMaxSizeMaxStringLengthJsonFilter filter = new SingleFullPathMaxSizeMaxStringLengthJsonFilter(128, i, -1, "/CVE_Items/cve/CVE_data_meta/NOT_ASSIGNER", FilterType.ANON);
		
			assertEquals(i, filter.getMaxSize());
			
			String process = filter.process(string);

			assertNotNull(i + " / " + string.length(), process);
			assertTrue(process.length() + " vs " + i, process.length() < i + 16);

			try {
				validate(process);
			} catch(Exception e) {
				System.out.println(process);
				fail();
			}
		}
	}
	
	@Test
	public void testMaxSizeCharsFullPath() throws IOException {
		String string = IOUtils.toString(getClass().getResourceAsStream("/json/maxSize/cve2006.json.gz.json"), StandardCharsets.UTF_8);

		for(int i = 2; i < string.length(); i++) {
			SingleFullPathMaxSizeMaxStringLengthJsonFilter filter = new SingleFullPathMaxSizeMaxStringLengthJsonFilter(128, i, -1, "/CVE_Items/cve/CVE_data_meta", FilterType.ANON);

			assertEquals(i, filter.getMaxSize());

			String process = filter.process(string);

			assertNotNull(i + " / " + string.length(), process);
			assertTrue(process.length() + " vs " + i, process.length() < i + 16);

			try {
				validate(process);
			} catch(JsonParseException e) {
				System.out.println(process);
				fail();
			}
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
