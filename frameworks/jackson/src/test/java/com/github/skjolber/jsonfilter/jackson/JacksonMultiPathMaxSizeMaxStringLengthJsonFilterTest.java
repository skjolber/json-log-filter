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

public class JacksonMultiPathMaxSizeMaxStringLengthJsonFilterTest extends AbstractJacksonJsonFilterTest {

	public JacksonMultiPathMaxSizeMaxStringLengthJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, null, null));
	}

	@Test
	public void passthrough_success() throws Exception {		
		Function<Integer, JsonFilter> maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, null, null);
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, null)).hasPassthrough();
		
		maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasPassthrough();
	}
	
	@Test
	public void anonymize() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, new String[]{DEFAULT_PATH}, null);
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH);
		
		maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEFAULT_PATH);
		
		maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, new String[]{DEEP_PATH1}, null);
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, new String[]{DEEP_PATH1}, null)).hasAnonymized(DEEP_PATH1);
	}

	@Test
	public void anonymizeWildcard() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, new String[]{DEFAULT_WILDCARD_PATH}, null);
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, new String[]{DEFAULT_WILDCARD_PATH}, null)).hasAnonymized(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void anonymizeAny() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, new String[]{DEFAULT_ANY_PATH}, null);
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH);
	}

	@Test
	public void prune() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, null, new String[]{DEFAULT_PATH});
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH);
		
		maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH});
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH})).hasPruned(DEFAULT_PATH);
		
		maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, null, new String[]{DEEP_PATH3});
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, new String[]{DEEP_PATH3})).hasPruned(DEEP_PATH3);
	}

	@Test
	public void pruneWildcard() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size,  null, new String[]{DEFAULT_WILDCARD_PATH});
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, new String[]{DEFAULT_WILDCARD_PATH})).hasPruned(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void pruneAny() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size,  null, new String[]{DEFAULT_ANY_PATH});
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(-1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH);
	}	

	@Test
	public void maxStringLength() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, null, null);
		assertThatMaxSize(maxSize, new JacksonMultiPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, null, null), UNICODE_FILTER).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH);
	}
	
	@Test
	public void maxStringLengthAnonymizePrune() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, new String[]{"/key1"}, new String[]{"/key3"});

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

	public static void main(String[] args) throws IOException {
		
		//assertThat(new SingleFullPathMaxSizeMaxStringLengthJsonFilter2(-1, -1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH);
		//assertThat(new SingleFullPathMaxSizeMaxStringLengthJsonFilter2(-1, -1, DEEP_PATH1, FilterType.ANON)).hasAnonymized(DEEP_PATH1);

		File file = new File("./../../support/test/src/main/resources/json/array/2d/arrayArray.json");
		String string = IOUtils.toString(file.toURI(), StandardCharsets.UTF_8);
		System.out.println(string);

		string = string + spaces(176);

		JacksonMultiPathMaxSizeMaxStringLengthJsonFilter filter = new JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, 176, null, null);

		String process = filter.process(string);
		System.out.println(process);
		System.out.println(process.length() + " siz");
	}
	
	private static String spaces(int length) {
		StringBuilder builder = new StringBuilder();
		
		for(int i = 0; i < length; i++) {
			builder.append(' ');
		}
		
		return builder.toString();
	}

}
