package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class SingleAnyPathMaxSizeMaxStringLengthJsonFilterTest extends DefaultJsonFilterTest {

	public SingleAnyPathMaxSizeMaxStringLengthJsonFilterTest() throws Exception {
		super();
	}
	
	@Test
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(128, size, -1,"//CVE_data_meta", FilterType.ANON));
	}
	
	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(128, size, -1,"//CVE_data_meta", FilterType.ANON));
	}

	@Test
	public void passthrough_success() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, ANY_PASSTHROUGH_XPATH, FilterType.PRUNE);

		assertThatMaxSize(maxSize, new SingleAnyPathMaxStringLengthJsonFilter(-1, -1, ANY_PASSTHROUGH_XPATH, FilterType.PRUNE)).hasPassthrough();
	}

	@Test
	public void anonymizeAny() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, DEFAULT_ANY_PATH, FilterType.ANON);
		
		assertThatMaxSize(maxSize, new SingleAnyPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_ANY_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_ANY_PATH);
	}

	@Test
	public void pruneAny() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, DEFAULT_ANY_PATH, FilterType.PRUNE);
		
		assertThatMaxSize(maxSize, new SingleAnyPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_ANY_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_ANY_PATH);
	}	

	@Test
	public void anonymizeAnyMaxStringLength() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, -1, "//key1", FilterType.ANON);
		
		assertThatMaxSize(maxSize, new SingleAnyPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, "//key1", FilterType.ANON)).hasAnonymized("//key1");
	}

	@Test
	public void anonymizeAnyMaxStringLengthMaxPathMatches() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, 1, "//key1", FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleAnyPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, 1, "//key1", FilterType.ANON)).hasAnonymized("//key1");
		
		maxSize = (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(4, size, 1, "//child1", FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleAnyPathMaxStringLengthJsonFilter(4, 1, "//child1", FilterType.ANON)).hasAnonymized("//child1");
		
		maxSize = (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(4, size, 2, "//child1", FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleAnyPathMaxStringLengthJsonFilter(4, 2, "//child1", FilterType.ANON)).hasAnonymized("//child1");
	}

	@Test
	public void pruneAnyMaxStringLength() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, -1, "//key3", FilterType.PRUNE);
		
		assertThatMaxSize(maxSize, new SingleAnyPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, "//key3", FilterType.PRUNE)).hasPruned("//key3");
	}		
	
	@Test
	public void pruneAnyMaxStringLengthMaxPathMatches() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, 1, "//key3", FilterType.PRUNE);
		
		assertThatMaxSize(maxSize, new SingleAnyPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, 1, "//key3", FilterType.PRUNE)).hasPruned("//key3");
	}
	
	@Test
	public void exception_returns_false() throws Exception {
		JsonFilter filter = new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(-1, -1, -1, DEFAULT_ANY_PATH, FilterType.PRUNE);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ByteArrayOutputStream()));
	}	
	
	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		JsonFilter filter = new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(-1, FULL.length - 4, -1, DEFAULT_ANY_PATH, FilterType.PRUNE);
		assertNull(filter.process(TRUNCATED));
		assertNull(filter.process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
		
		assertFalse(filter.process(FULL, 0, FULL.length - 3, new StringBuilder()));
		assertFalse(filter.process(new String(FULL).getBytes(StandardCharsets.UTF_8), 0, FULL.length - 3, new ByteArrayOutputStream()));
	}

}
