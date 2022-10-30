package com.github.skjolber.jsonfilter.jackson;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Function;

import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.github.skjolber.jsonfilter.JsonFilter;

public class JacksonMaxSizeMaxStringLengthJsonFilterTest extends AbstractJacksonJsonFilterTest {

	public JacksonMaxSizeMaxStringLengthJsonFilterTest() throws Exception {
		super(false);
	}
	
	@Test
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new JacksonMaxSizeMaxStringSizeJsonFilter(-1, size));
	}

	@Test
	public void passthrough_success() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new JacksonMaxSizeMaxStringSizeJsonFilter(-1, size);
		assertThatMaxSize(maxSize, new JacksonMaxStringLengthJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void maxStringLength() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new JacksonMaxSizeMaxStringSizeJsonFilter(DEFAULT_MAX_STRING_LENGTH, size);
		assertThatMaxSize(maxSize, new JacksonMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH), UNICODE_FILTER).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH);
	}
	
	@Test
	public void testConvenienceMethods() throws IOException {
		
		JsonFactory jsonFactory = mock(JsonFactory.class);
		when(jsonFactory.createGenerator(any(StringBuilderWriter.class))).thenThrow(new RuntimeException());
		when(jsonFactory.createGenerator(any(ByteArrayOutputStream.class))).thenThrow(new RuntimeException());
		
		testConvenienceMethods(
			new JacksonMaxStringLengthJsonFilter(-1) {
				public boolean process(final JsonParser parser, JsonGenerator generator) {
					return true;
				}
			}, 
			new JacksonMaxStringLengthJsonFilter(-1) {
				public boolean process(final JsonParser parser, JsonGenerator generator) {
					throw new RuntimeException();
				}
			},
			new JacksonMaxStringLengthJsonFilter(-1, jsonFactory) {
				public boolean process(final JsonParser parser, JsonGenerator generator) {
					throw new RuntimeException();
				}
			}
		);
	}

}
