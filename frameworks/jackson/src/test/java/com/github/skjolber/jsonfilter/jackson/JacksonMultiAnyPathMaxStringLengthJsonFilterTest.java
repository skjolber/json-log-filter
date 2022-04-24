package com.github.skjolber.jsonfilter.jackson;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

public class JacksonMultiAnyPathMaxStringLengthJsonFilterTest extends AbstractJacksonJsonFilterTest {

	public JacksonMultiAnyPathMaxStringLengthJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void testConstructor() {
		assertThrows(IllegalArgumentException.class, () -> {
			assertThat(new JacksonMultiAnyPathMaxStringLengthJsonFilter(-1, new String[] {PASSTHROUGH_XPATH}, null)).hasPassthrough();
		});
	}
	
	@Test
	public void passthrough_success() throws Exception {
		assertThat(new JacksonMultiAnyPathMaxStringLengthJsonFilter(-1, null, null)).hasPassthrough();
		assertThat(new JacksonMultiAnyPathMaxStringLengthJsonFilter(-1, new String[]{ANY_PASSTHROUGH_XPATH}, new String[]{ANY_PASSTHROUGH_XPATH})).hasPassthrough();
	}
	
	@Test
	public void anonymizeAny() throws Exception {
		assertThat(new JacksonMultiAnyPathMaxStringLengthJsonFilter(-1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH);
	}

	@Test
	public void pruneAny() throws Exception {
		assertThat(new JacksonMultiAnyPathMaxStringLengthJsonFilter(-1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH);
	}	

	@Test
	public void maxStringLength() throws Exception {
		assertThat(new JacksonMultiAnyPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, null, null), UNICODE_FILTER).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH);
	}
	
	@Test
	public void maxStringLengthAnonymizePrune() throws Exception {
		assertThat(new JacksonMultiAnyPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, new String[]{"//key1"}, new String[]{"//key3"}))
			.hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH)
			.hasPruned("//key3")
			.hasAnonymized("//key1");
	}
	
	@Test
	public void testConvenienceMethods() throws IOException {
		JsonFactory jsonFactory = mock(JsonFactory.class);
		when(jsonFactory.createGenerator(any(StringBuilderWriter.class))).thenThrow(new RuntimeException());
		when(jsonFactory.createGenerator(any(ByteArrayOutputStream.class))).thenThrow(new RuntimeException());
		
		testConvenienceMethods(
			new JacksonMultiAnyPathMaxStringLengthJsonFilter(-1, null, null) {
				public boolean process(final JsonParser parser, JsonGenerator generator) throws IOException {
					return true;
				}
			}, 
			new JacksonMultiAnyPathMaxStringLengthJsonFilter(-1, null, null) {
				public boolean process(final JsonParser parser, JsonGenerator generator) throws IOException {
					throw new RuntimeException();
				}
			},
			new JacksonMultiAnyPathMaxStringLengthJsonFilter(-1, null, null, jsonFactory) {
				public boolean process(final JsonParser parser, JsonGenerator generator) throws IOException {
					throw new RuntimeException();
				}
			}			
		);
	}
	
}
