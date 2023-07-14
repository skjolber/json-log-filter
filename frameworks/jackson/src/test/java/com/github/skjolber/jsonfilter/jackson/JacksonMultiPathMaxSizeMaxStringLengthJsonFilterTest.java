package com.github.skjolber.jsonfilter.jackson;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterFunction;

public class JacksonMultiPathMaxSizeMaxStringLengthJsonFilterTest extends AbstractJacksonJsonFilterTest {

	public JacksonMultiPathMaxSizeMaxStringLengthJsonFilterTest() throws Exception {
		super(false);
	}

	@Test
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, null, null));
	}

	@Test
	public void passthrough_success() throws Exception {		
		MaxSizeJsonFilterFunction maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, null, null);
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, null)).hasPassthrough();
		
		maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasPassthrough();
	}
	
	@Test
	public void anonymize() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, new String[]{DEFAULT_PATH}, null);
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH);
		
		maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEFAULT_PATH);
		
		maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, new String[]{DEEP_PATH1}, null);
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, new String[]{DEEP_PATH1}, null)).hasAnonymized(DEEP_PATH1);
	}

	@Test
	public void test() {
		String str = "[\n"
				+ "  1.1,\n"
				+ "  2.2,\n"
				+ "  3.3\n"
				+ "] ";
		
		
		MaxSizeJsonFilterFunction maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, new String[]{DEFAULT_PATH}, null);
		
		JsonFilter maxSize2 = maxSize.getMaxSize(str.length() );
		
		System.out.println(maxSize2.process(str));
	}
	
	@Test
	public void anonymizeWildcard() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, new String[]{DEFAULT_WILDCARD_PATH}, null);
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, new String[]{DEFAULT_WILDCARD_PATH}, null)).hasAnonymized(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void anonymizeAny() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, new String[]{DEFAULT_ANY_PATH}, null);
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH);
	}

	@Test
	public void prune() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, null, new String[]{DEFAULT_PATH});
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH);
		
		maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH});
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH})).hasPruned(DEFAULT_PATH);
		
		maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, null, new String[]{DEEP_PATH3});
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, new String[]{DEEP_PATH3})).hasPruned(DEEP_PATH3);
	}

	@Test
	public void pruneWildcard() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size,  null, new String[]{DEFAULT_WILDCARD_PATH});
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, new String[]{DEFAULT_WILDCARD_PATH})).hasPruned(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void pruneAny() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size,  null, new String[]{DEFAULT_ANY_PATH});
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH);
	}	

	@Test
	public void maxStringLength() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, null, null);
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, null, null)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH);
	}
	
	@Test
	public void maxStringLengthAnonymizePrune() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, new String[]{"/key1"}, new String[]{"/key3"});

		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, new String[]{"/key1"}, new String[]{"/key3"}))
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
				public boolean process(final JsonParser parser, JsonGenerator generator) throws IOException {
					return true;
				}
			}, 
			new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, null) {
				public boolean process(final JsonParser parser, JsonGenerator generator) throws IOException {
					throw new RuntimeException();
				}
			},
			new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, null, jsonFactory) {
				public boolean process(final JsonParser parser, JsonGenerator generator) throws IOException {
					throw new RuntimeException();
				}
			}			
		);
	}	

}
