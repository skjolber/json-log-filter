package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair.MaxSizeJsonFilterFunction;

public class SingleAnyPathMaxSizeMaxStringLengthJsonFilterTest extends DefaultJsonFilterTest {

	private static class MustContrainSingleAnyPathMaxSizeMaxStringLengthJsonFilter extends SingleAnyPathMaxSizeMaxStringLengthJsonFilter {

		public MustContrainSingleAnyPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
			super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
		}

		public MustContrainSingleAnyPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type) {
			super(maxStringLength, maxSize, maxPathMatches, expression, type);
		}

		@Override
		protected boolean mustConstrainMaxSize(int length) {
			return true;
		}
	};
	
	public SingleAnyPathMaxSizeMaxStringLengthJsonFilterTest() throws Exception {
		super();
	}
	
	@Test
	@ResourceLock(value = "jackson")
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(128, size, -1,"//CVE_data_meta", FilterType.ANON));
		
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(128, size, -1,"//description", FilterType.PRUNE));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(128, size, -1,"//CVE_data_meta", FilterType.ANON));
		
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(128, size, -1,"//cpe_match", FilterType.ANON));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(128, size, -1,"//impactScore", FilterType.ANON));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(128, size, -1,"//ASSIGNER", FilterType.ANON));
	}
	
	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(128, size, -1,"//CVE_data_meta", FilterType.ANON));
	}

	@Test
	public void passthrough_success() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, ANY_PASSTHROUGH_XPATH, FilterType.PRUNE);

		assertThatMaxSize(maxSize, new SingleAnyPathMaxStringLengthJsonFilter(-1, -1, ANY_PASSTHROUGH_XPATH, FilterType.PRUNE)).hasPassthrough();
	}

	@Test
	public void anonymizeAny() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleAnyPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, DEFAULT_ANY_PATH, FilterType.ANON);
		
		assertThatMaxSize(maxSize, new SingleAnyPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_ANY_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
	}

	@Test
	public void pruneAny() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleAnyPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, DEFAULT_ANY_PATH, FilterType.PRUNE);
		
		assertThatMaxSize(maxSize, new SingleAnyPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_ANY_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();
	}	

	@Test
	public void anonymizeAnyMaxStringLength() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, -1, "//key1", FilterType.ANON);
		
		assertThatMaxSize(maxSize, new SingleAnyPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, "//key1", FilterType.ANON)).hasAnonymized("//key1").hasAnonymizeMetrics();
	}

	@Test
	public void anonymizeAnyMaxStringLengthMaxPathMatches() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, 1, "//key1", FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleAnyPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, 1, "//key1", FilterType.ANON)).hasAnonymized("//key1").hasAnonymizeMetrics();
		
		maxSize = (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(4, size, 1, "//child1", FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleAnyPathMaxStringLengthJsonFilter(4, 1, "//child1", FilterType.ANON)).hasAnonymized("//child1").hasAnonymizeMetrics();
		
		maxSize = (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(4, size, 2, "//child1", FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleAnyPathMaxStringLengthJsonFilter(4, 2, "//child1", FilterType.ANON)).hasAnonymized("//child1").hasAnonymizeMetrics();
	}

	@Test
	public void pruneAnyMaxStringLength() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, -1, "//key3", FilterType.PRUNE);
		
		assertThatMaxSize(maxSize, new SingleAnyPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, "//key3", FilterType.PRUNE)).hasPruned("//key3").hasPruneMetrics();
	}
	
	@Test
	public void pruneAnyMaxStringLengthMaxPathMatches() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, 1, "//key3", FilterType.PRUNE);
		
		assertThatMaxSize(maxSize, new SingleAnyPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, 1, "//key3", FilterType.PRUNE)).hasPruned("//key3").hasPruneMetrics();
	}
	
	@Test
	public void exception_returns_false() throws Exception {
		JsonFilter filter = new SingleAnyPathMaxSizeMaxStringLengthJsonFilter(-1, -1, -1, DEFAULT_ANY_PATH, FilterType.PRUNE);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertNull(filter.process(new byte[] {}, 1, 1));
	}	

}
