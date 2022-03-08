package com.github.skjolber.jsonfilter.jackson;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

public class JacksonMaxSizeJsonFilterTest {

	private JsonFactory factory = new JsonFactory();

	@Test
	public void testMaxSize() throws IOException {
		String string = IOUtils.toString(getClass().getResourceAsStream("/json/maxSize/cve2006.json.gz.json"), StandardCharsets.UTF_8);

		for(int i = 2; i < string.length(); i++) {		
			JacksonMaxSizeJsonFilter filter = new JacksonMaxSizeJsonFilter(i);

			String process = filter.process(string);
	
			assertTrue(process.length() < i + 32);

			validate(process);
		}
	}

	private void validate(String process) throws IOException, JsonParseException {
		try (JsonParser parse = factory.createParser(process)) {
			while(parse.nextToken() != null);
		}
	}
	
}
