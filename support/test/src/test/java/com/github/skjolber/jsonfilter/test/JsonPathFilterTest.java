package com.github.skjolber.jsonfilter.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

/** 
 * Verify as much of the test resources as possible.
 *
 */

public class JsonPathFilterTest extends DefaultJsonFilterTest {

    private static final ParseContext CONTEXT = JsonPath.using(
            Configuration.builder()
                    .jsonProvider(new JacksonJsonNodeJsonProvider())
                    .mappingProvider(new JacksonMappingProvider())
                    .options(Option.SUPPRESS_EXCEPTIONS)
                    .options(Option.ALWAYS_RETURN_LIST)
                    .build());
    
	public JsonPathFilterTest() throws Exception {
		super(false, true, false);
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new JsonPathFilter(-1, null, null)).hasPassthrough();
		assertThat(new JsonPathFilter(-1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		JsonPathFilter filter = new JsonPathFilter(-1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}	
	
	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		JsonPathFilter filter = new JsonPathFilter(-1, null, null);
		assertNull(filter.process(TRUNCATED));
		assertNull(filter.process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
		
		assertFalse(filter.process(FULL, 0, FULL.length - 3, new StringBuilder()));
		// assertFalse(filter.process(new String(FULL).getBytes(StandardCharsets.UTF_8), 0, FULL.length - 3, new ByteArrayOutputStream()));
	}
	
	@Test
	public void exception_incorrect_level() throws Exception {
		JsonPathFilter filter = new JsonPathFilter(127, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertFalse(filter.process(INCORRECT_LEVEL, new StringBuilder()));
		assertNull(filter.process(INCORRECT_LEVEL.getBytes(StandardCharsets.UTF_8)));
	}
	
	@Test
	@Disabled
	public void anonymize() throws Exception {
		assertThat(new JsonPathFilter(-1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH);
		assertThat(new JsonPathFilter(-1, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEFAULT_PATH);
		assertThat(new JsonPathFilter(-1, new String[]{DEEP_PATH1, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEEP_PATH1);
	}
	/*
	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		assertThat(new JsonPathFilter(-1, 1, new String[]{"/key1"}, null)).hasAnonymized("/key1");
		
		assertThat(new JsonPathFilter(-1, 1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH);
		assertThat(new JsonPathFilter(-1, 2, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH);
	}
*/
	@Test
	@Disabled // currently does not work
	public void anonymizeWildcard() throws Exception {
		assertThat(new JsonPathFilter(-1, new String[]{DEFAULT_WILDCARD_PATH}, null)).hasAnonymized(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void anonymizeAny() throws Exception {
		assertThat(new JsonPathFilter(-1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH);
	}

	@Test
	@Disabled
	public void prune() throws Exception {
		assertThat(new JsonPathFilter(-1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH);
		assertThat(new JsonPathFilter(-1, null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH})).hasPruned(DEFAULT_PATH);
		assertThat(new JsonPathFilter(-1, null, new String[]{DEEP_PATH3, PASSTHROUGH_XPATH})).hasPruned(DEEP_PATH3);
	}
	
	/*
	@Test
	public void pruneMaxPathMatches() throws Exception {
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, 1, null, new String[]{"/key3"})).hasPruned("/key3");
		
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, 1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH);
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, 2, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH);
	}
	*/

	@Test
	@Disabled
	public void pruneWildcard() throws Exception {
		assertThat(new JsonPathFilter(-1, null, new String[]{DEFAULT_WILDCARD_PATH})).hasPruned(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void pruneAny() throws Exception {
		assertThat(new JsonPathFilter(-1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH);
	}	

	@Test
	public void maxStringLength() throws Exception {
		assertThat(new JsonPathFilter(DEFAULT_MAX_STRING_LENGTH, null, null)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH);
	}
	/*
	@Test
	public void maxStringLengthAnonymizePrune() throws Exception {
		assertThat(new JsonPathFilter(DEFAULT_MAX_STRING_LENGTH, 2, new String[]{"/key1"}, new String[]{"/key3"}))
			.hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH)
			.hasMaxPathMatches(2)
			.hasPruned("/key3")
			.hasAnonymized("/key1");
	}
*/
}
