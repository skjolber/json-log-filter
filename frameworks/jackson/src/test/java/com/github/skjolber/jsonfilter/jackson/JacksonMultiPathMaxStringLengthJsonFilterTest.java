package com.github.skjolber.jsonfilter.jackson;

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
import com.github.skjolber.jsonfilter.JsonFilterMetrics;

public class JacksonMultiPathMaxStringLengthJsonFilterTest extends AbstractJacksonJsonFilterTest {

	public JacksonMultiPathMaxStringLengthJsonFilterTest() throws Exception {
		super(true);
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, null)).hasPassthrough();
		assertThat(new JacksonMultiPathMaxStringLengthJsonFilter(-1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasPassthrough();
	}
	
	@Test
	public void anonymize() throws Exception {
		assertThat(new JacksonMultiPathMaxStringLengthJsonFilter(-1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH);
		assertThat(new JacksonMultiPathMaxStringLengthJsonFilter(-1, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEFAULT_PATH);
		assertThat(new JacksonMultiPathMaxStringLengthJsonFilter(-1, new String[]{DEEP_PATH1}, null)).hasAnonymized(DEEP_PATH1);
	}

	@Test
	public void anonymizeWildcard() throws Exception {
		assertThat(new JacksonMultiPathMaxStringLengthJsonFilter(-1, new String[]{DEFAULT_WILDCARD_PATH}, null)).hasAnonymized(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void anonymizeAny() throws Exception {
		assertThat(new JacksonMultiPathMaxStringLengthJsonFilter(-1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH);
	}

	@Test
	public void prune() throws Exception {
		assertThat(new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH);
		assertThat(new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH})).hasPruned(DEFAULT_PATH);
		assertThat(new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, new String[]{DEEP_PATH3})).hasPruned(DEEP_PATH3);
	}

	@Test
	public void pruneWildcard() throws Exception {
		assertThat(new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, new String[]{DEFAULT_WILDCARD_PATH})).hasPruned(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void pruneAny() throws Exception {
		assertThat(new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH);
	}	

	@Test
	public void maxStringLength() throws Exception {
		assertThat(new JacksonMultiPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, null, null)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH);
	}
	
	@Test
	public void maxStringLengthAnonymizePrune() throws Exception {
		assertThat(new JacksonMultiPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, new String[]{"/key1"}, new String[]{"/key3"}))
			.hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH)
			.hasPruned("/key3")
			.hasAnonymized("/key1");
	}
	
	@Test
	public void testConvenienceMethods() throws IOException {
		JsonFactory jsonFactory = mock(JsonFactory.class);
		when(jsonFactory.createGenerator(any(StringBuilderWriter.class))).thenThrow(new RuntimeException());
		when(jsonFactory.createGenerator(any(ByteArrayOutputStream.class))).thenThrow(new RuntimeException());
		
		testConvenienceMethods(
			new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, null) {
				public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) throws IOException {
					return true;
				}
			}, 
			new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, null) {
				public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) throws IOException {
					throw new RuntimeException();
				}
			},
			new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, null, jsonFactory) {
				public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) throws IOException {
					throw new RuntimeException();
				}
			}			
		);
	}	

}
