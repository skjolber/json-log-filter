package com.github.skjolber.jsonfilter.core;

import java.io.IOException;
import java.util.function.Function;

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
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleAnyPathMaxSizeJsonFilter(-1, size,"//CVE_data_meta", FilterType.ANON));
	}

	@Test
	public void anonymizeAny() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleAnyPathMaxSizeJsonFilter(-1, size, DEFAULT_ANY_PATH, FilterType.ANON);

		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(-1, DEFAULT_ANY_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_ANY_PATH);
	}

	@Test
	public void pruneAny() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleAnyPathMaxSizeJsonFilter(-1, size, DEFAULT_ANY_PATH, FilterType.PRUNE);
		
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(-1, DEFAULT_ANY_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_ANY_PATH);
	}

	@Test
	public void anonymizeAnyMaxPathMatches() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleAnyPathMaxSizeJsonFilter(1, size, "//key1", FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(1, "//key1", FilterType.ANON)).hasAnonymized("//key1");
		
		maxSize = (size) -> new SingleAnyPathMaxSizeJsonFilter(2, size,  "//child1", FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(2, "//child1", FilterType.ANON)).hasAnonymized("//child1");
	}

	@Test
	public void pruneAnyMaxPathMatches() throws Exception {
		Function<Integer, JsonFilter> maxSize = (size) -> new SingleAnyPathMaxSizeJsonFilter(1, size, "//key3", FilterType.PRUNE);
		
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(1, "//key3", FilterType.PRUNE)).hasPruned("//key3");
	}
	
}
