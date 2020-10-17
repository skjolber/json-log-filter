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
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class JacksonSinglePathMaxStringLengthJsonFilterTest extends AbstractJacksonJsonFilterTest {

	public JacksonSinglePathMaxStringLengthJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new JacksonSinglePathMaxStringLengthJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.ANON)).hasPassthrough();
	}
	
	@Test
	public void anonymize() throws Exception {
		assertThat(new JacksonSinglePathMaxStringLengthJsonFilter(-1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH);
		assertThat(new JacksonSinglePathMaxStringLengthJsonFilter(-1, DEEP_PATH1, FilterType.ANON)).hasAnonymized(DEEP_PATH1);
	}

	@Test
	public void anonymizeWildcard() throws Exception {
		assertThat(new JacksonSinglePathMaxStringLengthJsonFilter(-1, DEFAULT_WILDCARD_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void prune() throws Exception {
		assertThat(new JacksonSinglePathMaxStringLengthJsonFilter(-1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH);
		assertThat(new JacksonSinglePathMaxStringLengthJsonFilter(-1, DEEP_PATH3, FilterType.PRUNE)).hasPruned(DEEP_PATH3);
	}

	@Test
	public void pruneWildcard() throws Exception {
		assertThat(new JacksonSinglePathMaxStringLengthJsonFilter(-1, DEFAULT_WILDCARD_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_WILDCARD_PATH);
	}

	@Test
	public void maxStringLength() throws Exception {
		assertThat(new JacksonSinglePathMaxStringLengthJsonFilter(DEFAULT_MAX_LENGTH, PASSTHROUGH_XPATH, FilterType.PRUNE)).hasMaxStringLength(DEFAULT_MAX_LENGTH);
	}
	
	@Test
	public void testConvenienceMethods() throws IOException {
		JsonFactory jsonFactory = mock(JsonFactory.class);
		when(jsonFactory.createGenerator(any(StringBuilderWriter.class))).thenThrow(new RuntimeException());
		when(jsonFactory.createGenerator(any(ByteArrayOutputStream.class))).thenThrow(new RuntimeException());
		
		testConvenienceMethods(
			new JacksonSinglePathMaxStringLengthJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.ANON) {
				public boolean process(final JsonParser parser, JsonGenerator generator) throws IOException {
					return true;
				}
			}, 
			new JacksonSinglePathMaxStringLengthJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.ANON) {
				public boolean process(final JsonParser parser, JsonGenerator generator) throws IOException {
					throw new RuntimeException();
				}
			},
			new JacksonSinglePathMaxStringLengthJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.ANON, jsonFactory) {
				public boolean process(final JsonParser parser, JsonGenerator generator) throws IOException {
					throw new RuntimeException();
				}
			}
		);
		
	}	
	
	/*
	@Test
	public void maxStringLengthAnonymize() throws Exception {
		assertThat(new JacksonSinglePathMaxStringLengthJsonFilter(DEFAULT_MAX_LENGTH, DEFAULT_XPATH, FilterType.ANON))
			.hasMaxStringLength(DEFAULT_MAX_LENGTH)
			.hasAnonymized(DEFAULT_XPATH);
	}

	@Test
	public void maxStringLengthPrune() throws Exception {
		assertThat(new JacksonSinglePathMaxStringLengthJsonFilter(DEFAULT_MAX_LENGTH, DEFAULT_XPATH, FilterType.PRUNE))
			.hasMaxStringLength(DEFAULT_MAX_LENGTH)
			.hasPruned(DEFAULT_XPATH);
	}
*/
}
