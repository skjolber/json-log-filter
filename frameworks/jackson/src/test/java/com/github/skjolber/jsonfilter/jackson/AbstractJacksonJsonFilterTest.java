package com.github.skjolber.jsonfilter.jackson;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonFactory;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.jackson.AbstractJacksonJsonFilterTest.MyJacksonJsonFilterTest;

public class AbstractJacksonJsonFilterTest {

	public class MyJacksonJsonFilterTest extends AbstractJacksonJsonFilter {

		public MyJacksonJsonFilterTest(int maxStringLength, int maxSize, String pruneJson, String anonymizeJson, String truncateJsonString,
				JsonFactory jsonFactory) {
			super(maxStringLength, maxSize, pruneJson, anonymizeJson, truncateJsonString, jsonFactory);
		}

		public MyJacksonJsonFilterTest(JsonFactory jsonFactory) {
			super(jsonFactory);
		}

		@Override
		public boolean process(char[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics filterMetrics) {
			throw new RuntimeException();
		}

		@Override
		public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output,
				JsonFilterMetrics filterMetrics) {
			throw new RuntimeException();
		}
		
	}
	
	private JsonFactory jsonFactory = new JsonFactory();

	@org.junit.jupiter.api.Test
	public void test() {
		
		MyJacksonJsonFilterTest filter = new MyJacksonJsonFilterTest(123, 456, "//test", "//me", "abc", jsonFactory);
		
		assertArrayEquals(filter.getPruneJsonValue(), "//test".toCharArray());
		assertArrayEquals(filter.getAnonymizeJsonValue(), "//me".toCharArray());
		assertArrayEquals(filter.getTruncateStringValue(), "abc".toCharArray());
	}	
}
