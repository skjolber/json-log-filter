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
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilterTest extends AbstractJacksonJsonFilterTest {

	public JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new JacksonMaxSizeJsonFilter(size));
	}
	
	@Test
	public void passthrough_success() throws Exception {
		assertThat(new JacksonSingleFullPathMaxStringLengthJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.ANON)).hasPassthrough();
	}
	
	@Test
	public void anonymize() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilter(size, -1, DEFAULT_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new JacksonSingleFullPathMaxStringLengthJsonFilter(-1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH);
		
		maxSize = (size) -> new JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilter(size, -1, DEEP_PATH1, FilterType.ANON);
		assertThatMaxSize(maxSize, new JacksonSingleFullPathMaxStringLengthJsonFilter(-1, DEEP_PATH1, FilterType.ANON)).hasAnonymized(DEEP_PATH1);
	}

	@Test
	public void anonymizeWildcard() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilter(size, -1, DEFAULT_WILDCARD_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new JacksonSingleFullPathMaxStringLengthJsonFilter(-1, DEFAULT_WILDCARD_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void prune() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilter(size, -1, DEFAULT_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new JacksonSingleFullPathMaxStringLengthJsonFilter(-1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH);
		
		maxSize = (size) -> new JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilter(size, -1, DEEP_PATH3, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new JacksonSingleFullPathMaxStringLengthJsonFilter(-1, DEEP_PATH3, FilterType.PRUNE)).hasPruned(DEEP_PATH3);
	}

	@Test
	public void pruneWildcard() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilter(size, -1, DEFAULT_WILDCARD_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new JacksonSingleFullPathMaxStringLengthJsonFilter(-1, DEFAULT_WILDCARD_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_WILDCARD_PATH);
	}

	@Test
	public void maxStringLength() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, PASSTHROUGH_XPATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new JacksonSingleFullPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, PASSTHROUGH_XPATH, FilterType.PRUNE), UNICODE_FILTER).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH);
	}
	
	@Test
	public void testConvenienceMethods() throws IOException {
		JsonFactory jsonFactory = mock(JsonFactory.class);
		when(jsonFactory.createGenerator(any(StringBuilderWriter.class))).thenThrow(new RuntimeException());
		when(jsonFactory.createGenerator(any(ByteArrayOutputStream.class))).thenThrow(new RuntimeException());
		
		testConvenienceMethods(
			new JacksonSingleFullPathMaxStringLengthJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.ANON) {
				public boolean process(final JsonParser parser, JsonGenerator generator) {
					return true;
				}
			}, 
			new JacksonSingleFullPathMaxStringLengthJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.ANON) {
				public boolean process(final JsonParser parser, JsonGenerator generator) {
					throw new RuntimeException();
				}
			},
			new JacksonSingleFullPathMaxStringLengthJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.ANON, jsonFactory) {
				public boolean process(final JsonParser parser, JsonGenerator generator) {
					throw new RuntimeException();
				}
			}
		);
		
	}	

}
