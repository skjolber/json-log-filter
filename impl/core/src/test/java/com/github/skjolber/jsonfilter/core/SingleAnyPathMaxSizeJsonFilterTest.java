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

public class SingleAnyPathMaxSizeJsonFilterTest extends DefaultJsonFilterTest {

	public SingleAnyPathMaxSizeJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleAnyPathMaxSizeJsonFilter(size, -1,"//CVE_data_meta", FilterType.ANON));
	}

	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new SingleAnyPathMaxSizeJsonFilter(size, -1,"//CVE_data_meta", FilterType.ANON));
	}

	@Test
	public void passthrough_success() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleAnyPathMaxSizeJsonFilter(size, -1, ANY_PASSTHROUGH_XPATH, FilterType.ANON);
		
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(-1, ANY_PASSTHROUGH_XPATH, FilterType.PRUNE)).hasPassthrough();
	}

	@Test
	public void anonymizeAny() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleAnyPathMaxSizeJsonFilter(size, -1, DEFAULT_ANY_PATH, FilterType.ANON);

		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(-1, DEFAULT_ANY_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_ANY_PATH);
	}

	@Test
	public void pruneAny() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleAnyPathMaxSizeJsonFilter(size, -1, DEFAULT_ANY_PATH, FilterType.PRUNE);
		
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(-1, DEFAULT_ANY_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_ANY_PATH);
	}

	@Test
	public void anonymizeAnyMaxPathMatches() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleAnyPathMaxSizeJsonFilter(size, 1, "//key1", FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(1, "//key1", FilterType.ANON)).hasAnonymized("//key1");
		
		maxSize = (size) -> new SingleAnyPathMaxSizeJsonFilter(size, 2,  "//child1", FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(2, "//child1", FilterType.ANON)).hasAnonymized("//child1");
	}

	@Test
	public void pruneAnyMaxPathMatches() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleAnyPathMaxSizeJsonFilter(size, 1, "//key3", FilterType.PRUNE);
		
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(1, "//key3", FilterType.PRUNE)).hasPruned("//key3");
	}
	
	@Test
	public void exception_returns_false() throws Exception {
		JsonFilter filter = new SingleAnyPathMaxSizeJsonFilter(-1, -1, DEFAULT_ANY_PATH, FilterType.PRUNE);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ByteArrayOutputStream()));
	}	
	
	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		JsonFilter filter = new SingleAnyPathMaxSizeJsonFilter(-1, FULL.length - 4, DEFAULT_ANY_PATH, FilterType.PRUNE);
		assertNull(filter.process(TRUNCATED));
		assertNull(filter.process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
		
		assertFalse(filter.process(FULL, 0, FULL.length - 3, new StringBuilder()));
		assertFalse(filter.process(new String(FULL).getBytes(StandardCharsets.UTF_8), 0, FULL.length - 3, new ByteArrayOutputStream()));
	}
	
	
	public static void main(String[] args) throws IOException {
		
		//assertThat(new SingleFullPathMaxSizeMaxStringLengthJsonFilter2(-1, -1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH);
		//assertThat(new SingleFullPathMaxSizeMaxStringLengthJsonFilter2(-1, -1, DEEP_PATH1, FilterType.ANON)).hasAnonymized(DEEP_PATH1);

		File file = new File("./../../support/test/src/main/resources/json/array/1d/objectValueNumberArray.json");
		String string = IOUtils.toString(file.toURI(), StandardCharsets.UTF_8);
		
		SingleAnyPathJsonFilter infiniteFilter = new SingleAnyPathJsonFilter(-1, DEFAULT_ANY_PATH, FilterType.ANON);
		
		String infinite = infiniteFilter.process(string);
		
		System.out.println(infinite);
		
		int size = infinite.length();
		
		string = string + "     ";
		
		SingleAnyPathMaxSizeJsonFilter filter = new SingleAnyPathMaxSizeJsonFilter(size, -1, DEFAULT_ANY_PATH, FilterType.ANON);

		String process = filter.process(string);
		System.out.println(process);
		System.out.println(process.length() + " size for max " + size);
	}
	
	

}
