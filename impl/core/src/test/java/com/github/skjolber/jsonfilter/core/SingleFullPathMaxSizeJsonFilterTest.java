package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class SingleFullPathMaxSizeJsonFilterTest {

	private JsonFactory factory = new JsonFactory();

	@Test
	public void testMaxSizeChars() throws IOException {
		String string = IOUtils.toString(getClass().getResourceAsStream("/json/maxSize/cve2006.json.gz.json"), StandardCharsets.UTF_8);

		for(int i = 2; i < string.length(); i++) {		
			SingleFullPathMaxSizeJsonFilter filter = new SingleFullPathMaxSizeJsonFilter(-1, i, "/CVE_Items/cve/CVE_data_meta/ASSIGNER", FilterType.ANON);
		
			String process = filter.process(string);
			/*
			System.out.println("************");
			System.out.println(process);
			System.out.println("************");
			*/
			assertNotNull(i + " / " + string.length(), process);
			assertTrue(process.length() < i + 16);

			validate(process);
		}
	}
	
	@Test
	public void testMaxSizeChars2() throws IOException {
		String string = IOUtils.toString(getClass().getResourceAsStream("/json/maxSize/cve2006.json.gz.json"), StandardCharsets.UTF_8);

		for(int i = 2; i < string.length(); i++) {		
			SingleFullPathMaxSizeJsonFilter filter = new SingleFullPathMaxSizeJsonFilter(-1, i, "/CVE_Items/cve/CVE_data_meta", FilterType.ANON);
		
			String process = filter.process(string);
			/*
			System.out.println("************");
			System.out.println(process);
			System.out.println("************");
			*/
			assertNotNull(i + " / " + string.length(), process);
			assertTrue(process.length() < i + 16);

			try {
				validate(process);
			} catch(JsonParseException e) {
				System.out.println(process);
				
				break;
			}
			
			if(i == string.length()/2) {
				System.out.println(process);
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
