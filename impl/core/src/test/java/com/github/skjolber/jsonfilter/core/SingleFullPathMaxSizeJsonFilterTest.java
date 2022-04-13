package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class SingleFullPathMaxSizeJsonFilterTest extends DefaultJsonFilterTest {

	public SingleFullPathMaxSizeJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleFullPathMaxSizeJsonFilter(-1, size, PASSTHROUGH_XPATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.ANON)).hasPassthrough();
	}
	
	@Test
	public void anonymize() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleFullPathMaxSizeJsonFilter(-1, size, DEFAULT_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathJsonFilter(-1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH);
		
		maxSize = (size) -> new SingleFullPathMaxSizeJsonFilter(-1, size, DEEP_PATH1, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathJsonFilter(-1, DEEP_PATH1, FilterType.ANON)).hasAnonymized(DEEP_PATH1);
	}
	
	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleFullPathMaxSizeJsonFilter(1, size, "/key1", FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathJsonFilter(1, "/key1", FilterType.ANON)).hasAnonymized("/key1");
		
		assertThat(new SingleFullPathJsonFilter(1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH);
		assertThat(new SingleFullPathJsonFilter(2, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH);
	}	

	@Test
	public void anonymizeWildcard() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleFullPathMaxSizeJsonFilter(-1, size, DEFAULT_WILDCARD_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathJsonFilter(-1, DEFAULT_WILDCARD_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void prune() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleFullPathMaxSizeJsonFilter(-1, size, DEFAULT_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathJsonFilter(-1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH);
		
		maxSize = (size) -> new SingleFullPathMaxSizeJsonFilter(-1, size, DEEP_PATH3, FilterType.PRUNE);
		assertThat(new SingleFullPathJsonFilter(-1, DEEP_PATH3, FilterType.PRUNE)).hasPruned(DEEP_PATH3);
	}
	
	@Test
	public void pruneMaxPathMatches() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleFullPathMaxSizeJsonFilter(1, size, "/key3", FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathJsonFilter(1, "/key3", FilterType.PRUNE)).hasPruned("/key3");
		
		maxSize = (size) -> new SingleFullPathMaxSizeJsonFilter(1, size, DEFAULT_PATH, FilterType.PRUNE);
		assertThat(new SingleFullPathJsonFilter(1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH);
		
		maxSize = (size) -> new SingleFullPathMaxSizeJsonFilter(2, size, DEFAULT_PATH, FilterType.PRUNE);
		assertThat(new SingleFullPathJsonFilter(2, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH);
	}	

	@Test
	public void pruneWildcard() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleFullPathMaxSizeJsonFilter(-1, size, DEFAULT_WILDCARD_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathJsonFilter(-1, DEFAULT_WILDCARD_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_WILDCARD_PATH);
	}
	
	public static void main(String[] args) throws IOException {
		SingleFullPathJsonFilter filter = new SingleFullPathJsonFilter(1, DEFAULT_PATH, FilterType.ANON);
		
		File file = new File("../../support/test/src/main/resources/json/text/single/object1xKeyDeepEscaped.json");
		String string = IOUtils.toString(file.toURI(), StandardCharsets.UTF_8);
		
		System.out.println(string);
		
		String process = filter.process(string);
		System.out.println(process);
	}
}
