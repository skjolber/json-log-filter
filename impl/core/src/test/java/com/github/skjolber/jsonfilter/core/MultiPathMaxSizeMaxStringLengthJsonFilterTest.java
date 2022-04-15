package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class MultiPathMaxSizeMaxStringLengthJsonFilterTest extends DefaultJsonFilterTest {

	public MultiPathMaxSizeMaxStringLengthJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(128, size, -1, new String[] {"/CVE_Items/cve/CVE_data_meta"}, null));
	}
	
	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(128, size, -1, new String[] {"/CVE_Items/cve/CVE_data_meta"}, null));
	}
	
	@Test
	public void passthrough_success() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, null, null);
		assertThatMaxSize(maxSize, new MultiPathMaxStringLengthJsonFilter(-1, -1, null, null)).hasPassthrough();
		
		maxSize = (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertThatMaxSize(maxSize, new MultiPathMaxStringLengthJsonFilter(-1, -1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasPassthrough();
	}
	
	@Test
	public void anonymize() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, new String[]{DEFAULT_PATH}, null);
		assertThatMaxSize(maxSize, new MultiPathMaxStringLengthJsonFilter(-1, -1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH);
		
		maxSize = (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, -1, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEFAULT_PATH);
		
		maxSize = (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, new String[]{DEEP_PATH1, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, -1, new String[]{DEEP_PATH1, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEEP_PATH1);
	}
	
	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, new String[]{"/key1"}, null);
		assertThatMaxSize(maxSize, new MultiPathMaxStringLengthJsonFilter(-1, 1, new String[]{"/key1"}, null)).hasAnonymized("/key1");
		

		maxSize = (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, new String[]{DEFAULT_PATH}, null);
		assertThatMaxSize(maxSize, new MultiPathMaxStringLengthJsonFilter(-1, 1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH);
		
		maxSize = (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, 2, new String[]{DEFAULT_PATH}, null);
		assertThatMaxSize(maxSize, new MultiPathMaxStringLengthJsonFilter(-1, 2, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH);
	}

	@Test
	public void anonymizeWildcard() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, new String[]{DEFAULT_WILDCARD_PATH}, null);
		assertThatMaxSize(maxSize, new MultiPathMaxStringLengthJsonFilter(-1, -1, new String[]{DEFAULT_WILDCARD_PATH}, null)).hasAnonymized(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void anonymizeAny() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, new String[]{DEFAULT_ANY_PATH}, null);
		assertThatMaxSize(maxSize, new MultiPathMaxStringLengthJsonFilter(-1, -1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH);
	}

	@Test
	public void prune() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, null, new String[]{DEFAULT_PATH});
		assertThatMaxSize(maxSize, new MultiPathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH);
		
		maxSize = (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH});
		assertThatMaxSize(maxSize, new MultiPathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH})).hasPruned(DEFAULT_PATH);
		
		maxSize = (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, null, new String[]{DEEP_PATH3, PASSTHROUGH_XPATH});
		assertThatMaxSize(maxSize, new MultiPathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEEP_PATH3, PASSTHROUGH_XPATH})).hasPruned(DEEP_PATH3);
	}
	
	@Test
	public void pruneMaxPathMatches() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, null, new String[]{"/key3"});
		assertThatMaxSize(maxSize, new MultiPathMaxStringLengthJsonFilter(-1, 1, null, new String[]{"/key3"})).hasPruned("/key3");
		
		maxSize = (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, null, new String[]{DEFAULT_PATH});
		assertThatMaxSize(maxSize, new MultiPathMaxStringLengthJsonFilter(-1, 1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH);
		
		maxSize = (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, 2, null, new String[]{DEFAULT_PATH});
		assertThatMaxSize(maxSize, new MultiPathMaxStringLengthJsonFilter(-1, 2, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH);
	}

	@Test
	public void pruneWildcard() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, null, new String[]{DEFAULT_WILDCARD_PATH});
		assertThatMaxSize(maxSize, new MultiPathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEFAULT_WILDCARD_PATH})).hasPruned(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void pruneAny() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, null, new String[]{DEFAULT_ANY_PATH});
		assertThatMaxSize(maxSize, new MultiPathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH);
	}	

	@Test
	public void maxStringLength() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, -1, null, null);
		assertThatMaxSize(maxSize, new MultiPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, null, null)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH);
	}
	
	@Test
	public void maxStringLengthAnonymizePrune() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new MultiPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, 2, new String[]{"/key1"}, new String[]{"/key3"});

		assertThatMaxSize(maxSize, new MultiPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, 2, new String[]{"/key1"}, new String[]{"/key3"}))
			.hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH)
			.hasMaxPathMatches(2)
			.hasPruned("/key3")
			.hasAnonymized("/key1");
	}

	@Test
	public void exception_returns_false() throws Exception {
		MultiPathMaxStringLengthJsonFilter filter = new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, -1, -1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ByteArrayOutputStream()));
	}	
	
	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		MultiPathMaxStringLengthJsonFilter filter = new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, FULL.length - 4, -1, null, null);
		assertNull(filter.process(TRUNCATED));
		assertNull(filter.process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
		
		assertFalse(filter.process(FULL, 0, FULL.length - 3, new StringBuilder()));
		assertFalse(filter.process(new String(FULL).getBytes(StandardCharsets.UTF_8), 0, FULL.length - 3, new ByteArrayOutputStream()));
	}
	
	@Test
	public void exception_incorrect_level() throws Exception {
		MultiPathMaxStringLengthJsonFilter filter = new MultiPathMaxSizeMaxStringLengthJsonFilter(-1, FULL.length - 4, 127, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertFalse(filter.process(INCORRECT_LEVEL, new StringBuilder()));
		assertNull(filter.process(INCORRECT_LEVEL.getBytes(StandardCharsets.UTF_8)));
	}
	
}
