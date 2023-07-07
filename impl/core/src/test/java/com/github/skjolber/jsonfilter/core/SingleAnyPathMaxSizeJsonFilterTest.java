package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.MaxSizeJsonFilterFunction;

public class SingleAnyPathMaxSizeJsonFilterTest extends DefaultJsonFilterTest {

	public SingleAnyPathMaxSizeJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleAnyPathMaxSizeJsonFilter(size, -1,"//description", FilterType.PRUNE));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleAnyPathMaxSizeJsonFilter(size, -1,"//description", FilterType.ANON));
		
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleAnyPathMaxSizeJsonFilter(size, -1,"//cpe_match", FilterType.ANON));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleAnyPathMaxSizeJsonFilter(size, -1,"//impactScore", FilterType.ANON));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleAnyPathMaxSizeJsonFilter(size, -1,"//ASSIGNER", FilterType.ANON));
	}

	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new SingleAnyPathMaxSizeJsonFilter(size, -1,"//CVE_data_meta", FilterType.ANON));
	}

	@Test
	public void passthrough_success() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new SingleAnyPathMaxSizeJsonFilter(size, -1, ANY_PASSTHROUGH_XPATH, FilterType.ANON);
		
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(-1, ANY_PASSTHROUGH_XPATH, FilterType.PRUNE)).hasPassthrough();
	}

	@Test
	public void anonymizeAny() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new SingleAnyPathMaxSizeJsonFilter(size, -1, DEFAULT_ANY_PATH, FilterType.ANON);

		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(-1, DEFAULT_ANY_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
	}

	@Test
	public void pruneAny() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new SingleAnyPathMaxSizeJsonFilter(size, -1, DEFAULT_ANY_PATH, FilterType.PRUNE);
		
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(-1, DEFAULT_ANY_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();
	}

	@Test
	public void anonymizeAnyMaxPathMatches() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new SingleAnyPathMaxSizeJsonFilter(size, 1, "//key1", FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(1, "//key1", FilterType.ANON)).hasAnonymized("//key1").hasAnonymizeMetrics();
		
		maxSize = (size) -> new SingleAnyPathMaxSizeJsonFilter(size, 2,  "//child1", FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(2, "//child1", FilterType.ANON)).hasAnonymized("//child1").hasAnonymizeMetrics();
		
		maxSize = (size) -> new SingleAnyPathMaxSizeJsonFilter(size, 1, DEFAULT_ANY_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(1, DEFAULT_ANY_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
	}
	
	@Test
	public void pruneAnyMaxPathMatches() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new SingleAnyPathMaxSizeJsonFilter(size, 1, "//key3", FilterType.PRUNE);
		
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(1, "//key3", FilterType.PRUNE)).hasPruned("//key3").hasPruneMetrics();
		
		maxSize = (size) -> new SingleAnyPathMaxSizeJsonFilter(size, 1, DEFAULT_ANY_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(1, DEFAULT_ANY_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();

	}
	
	@Test
	public void exception_returns_false() throws Exception {
		JsonFilter filter = new SingleAnyPathMaxSizeJsonFilter(-1, -1, DEFAULT_ANY_PATH, FilterType.PRUNE);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertNull(filter.process(new byte[] {}, 1, 1));
	}	
	
}
