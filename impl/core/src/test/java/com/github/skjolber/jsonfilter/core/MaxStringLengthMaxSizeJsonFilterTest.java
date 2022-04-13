package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class MaxStringLengthMaxSizeJsonFilterTest extends DefaultJsonFilterTest {

	public MaxStringLengthMaxSizeJsonFilterTest() throws Exception {
		super();
	}

	private JsonFactory factory = new JsonFactory();

	@Test
	public void testMaxSizeChars() throws IOException {
		String string = IOUtils.toString(getClass().getResourceAsStream("/json/maxSize/cve2006.json.gz.json"), StandardCharsets.UTF_8);

		for(int i = 2; i < string.length(); i++) {		
			MaxStringLengthMaxSizeJsonFilter filter = new MaxStringLengthMaxSizeJsonFilter(128, i);
		
			String process = filter.process(string);

			assertNotNull(i + " / " + string.length(), process);
			if(process.length() >= i + 16) {
				System.out.println(process);
				fail(process.length() + " vs " + i);
			}

			try {
				validate(process);
			} catch(Exception e) {
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
	
	@Test
	public void passthrough_success() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new MaxStringLengthMaxSizeJsonFilter(-1, size);

		assertThatMaxSize(maxSize, new MaxStringLengthJsonFilter(-1)).hasPassthrough();
	}

	
	@Test
	public void maxStringLength() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new MaxStringLengthMaxSizeJsonFilter(DEFAULT_MAX_STRING_LENGTH, size);
		
		assertThatMaxSize( maxSize, new MaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH);
	}
}
