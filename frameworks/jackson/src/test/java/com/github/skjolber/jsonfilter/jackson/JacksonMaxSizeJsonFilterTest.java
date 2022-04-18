package com.github.skjolber.jsonfilter.jackson;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class JacksonMaxSizeJsonFilterTest extends AbstractJacksonJsonFilterTest {

	public JacksonMaxSizeJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new JacksonMaxSizeJsonFilter(size));
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new JacksonMaxSizeJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new JacksonMaxSizeJsonFilter(-1).process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(new JacksonMaxSizeJsonFilter(-1).process(new byte[] {}, 1, 1, new StringBuilder()));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		assertNull(new JacksonMaxSizeJsonFilter(DEFAULT_MAX_SIZE).process(TRUNCATED));
		assertNull(new JacksonMaxSizeJsonFilter(DEFAULT_MAX_SIZE).process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}
	
	@Test
	public void maxSize() throws Exception {
		assertThat(new JacksonMaxSizeJsonFilter(DEFAULT_MAX_SIZE)).hasMaxSize(DEFAULT_MAX_SIZE);
	}
	
	public static void main(String[] args) throws Exception {
		
		//assertThat(new SingleFullPathMaxSizeMaxStringLengthJsonFilter2(-1, -1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH);
		//assertThat(new SingleFullPathMaxSizeMaxStringLengthJsonFilter2(-1, -1, DEEP_PATH1, FilterType.ANON)).hasAnonymized(DEEP_PATH1);

		File file = new File("/home/skjolber/git/json-log-filter/frameworks/jackson/src/test/resources/test.json");
		String string = IOUtils.toString(file.toURI(), StandardCharsets.UTF_8);
		System.out.println(string);

		JsonFactory jsonFactory = new JsonFactory();

		JsonParser parser = jsonFactory.createParser(string.toCharArray(), 0, string.length());

		char[] offsets = new char[string.length()];
		for(int i = 0; i < offsets.length; i++) {
			offsets[i] = ' ';
		}
		
		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}

			System.out.println(nextToken + " " + parser.currentLocation().getCharOffset());
			
			offsets[ (int)parser.currentLocation().getCharOffset()] = '^';
			
			if(nextToken == JsonToken.VALUE_STRING) {
				parser.getTextLength();
			} else if(nextToken == JsonToken.VALUE_NUMBER_INT) {
				parser.getNumberValue();
			}
			System.out.println(nextToken + " " + parser.currentLocation().getCharOffset());
			offsets[ (int)parser.currentLocation().getCharOffset()] = '|';
			
		}
	
		System.out.println(string);
		System.out.println(new String(offsets));
	}

}
