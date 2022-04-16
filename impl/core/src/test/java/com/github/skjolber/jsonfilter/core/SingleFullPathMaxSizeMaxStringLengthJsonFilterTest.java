package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class SingleFullPathMaxSizeMaxStringLengthJsonFilterTest extends DefaultJsonFilterTest {

	public SingleFullPathMaxSizeMaxStringLengthJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(128, size, -1,"/CVE_Items/cve/CVE_data_meta", FilterType.ANON));
	}
	
	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(128, size, -1,"/CVE_Items/cve/CVE_data_meta", FilterType.ANON));
	}
	
	
	@Test
	public void passthrough_success() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, PASSTHROUGH_XPATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, -1, PASSTHROUGH_XPATH, FilterType.ANON)).hasPassthrough();
	}

	@Test
	public void anonymize() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, DEFAULT_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH);
		
		maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, DEEP_PATH1, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEEP_PATH1, FilterType.ANON)).hasAnonymized(DEEP_PATH1);
	}

	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, "/key1", FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 1, "/key1", FilterType.ANON)).hasAnonymized("/key1");
		
		maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, DEFAULT_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH);
		
		maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 2, DEFAULT_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 2, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH);
	}
	
	@Test
	public void anonymizeWildcard() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, DEFAULT_WILDCARD_PATH, FilterType.ANON);
		
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_WILDCARD_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void prune() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, DEFAULT_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH);
		
		maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, DEEP_PATH3, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEEP_PATH3, FilterType.PRUNE)).hasPruned(DEEP_PATH3);
	}

	@Test
	public void pruneMaxPathMatches() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, "/key3", FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 1, "/key3", FilterType.PRUNE)).hasPruned("/key3");
		
		maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, DEFAULT_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH);
		
		maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 2, DEFAULT_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 2, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH);
	}

	@Test
	public void pruneWildcard() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, "/key3", FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 1, "/key3", FilterType.PRUNE)).hasPruned("/key3");
	}

	@Test
	public void maxStringLength() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, -1, PASSTHROUGH_XPATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, PASSTHROUGH_XPATH, FilterType.PRUNE)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH);
	}

	@Test
	public void maxStringLengthMaxStringLength() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, 1, "/key3", FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, 1, "/key3", FilterType.PRUNE)).hasPruned("/key3");
	}

	@Test
	public void exception_returns_false() throws Exception {
		JsonFilter filter = new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, -1, -1, PASSTHROUGH_XPATH, FilterType.PRUNE);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertNull(filter.process(new byte[] {}, 1, 1));
	}	
	
	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		JsonFilter filter = new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, FULL.length - 4, -1, PASSTHROUGH_XPATH, FilterType.PRUNE);
		assertNull(filter.process(TRUNCATED));
		assertNull(filter.process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
		
		assertFalse(filter.process(FULL, 0, FULL.length - 3, new StringBuilder()));
		assertNull(filter.process(new String(FULL).getBytes(StandardCharsets.UTF_8), 0, FULL.length - 3));
	}
	
	@Test
	public void exception_incorrect_level() throws Exception {
		JsonFilter filter = new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, FULL.length - 4, 127, PASSTHROUGH_XPATH, FilterType.PRUNE);
		assertFalse(filter.process(INCORRECT_LEVEL, new StringBuilder()));
		assertNull(filter.process(INCORRECT_LEVEL.getBytes(StandardCharsets.UTF_8)));
	}
	
	public static void main(String[] args) throws IOException {
		
		//assertThat(new SingleFullPathMaxSizeMaxStringLengthJsonFilter2(-1, -1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH);
		//assertThat(new SingleFullPathMaxSizeMaxStringLengthJsonFilter2(-1, -1, DEEP_PATH1, FilterType.ANON)).hasAnonymized(DEEP_PATH1);

		
		JsonFilter filter = new SingleFullPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, 30, -1, PASSTHROUGH_XPATH, FilterType.ANON);
		
		File file = new File("./../../support/test/src/main/resources/json/text/single/object1xKeyLongEscapedUnicode.json");
		String string = IOUtils.toString(file.toURI(), StandardCharsets.UTF_8);
		
		System.out.println(string);
		
		String process = filter.process(string);
		System.out.println(process);
		System.out.println(process.length() + " size");
	}
}
