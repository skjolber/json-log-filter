package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair.MaxSizeJsonFilterFunction;

public class SingleAnyPathMaxSizeJsonFilterTest extends DefaultJsonFilterTest {

	private static class MustContrainSingleAnyPathMaxSizeJsonFilter extends SingleAnyPathMaxSizeJsonFilter {

		public MustContrainSingleAnyPathMaxSizeJsonFilter(int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
			super(maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
		}

		public MustContrainSingleAnyPathMaxSizeJsonFilter(int maxSize, int maxPathMatches, String expression, FilterType type) {
			super(maxSize, maxPathMatches, expression, type);
		}

		@Override
		protected boolean mustConstrainMaxSize(int length) {
			return true;
		}
	};
	
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
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleAnyPathMaxSizeJsonFilter(size, -1, ANY_PASSTHROUGH_XPATH, FilterType.ANON);
		
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(-1, ANY_PASSTHROUGH_XPATH, FilterType.PRUNE)).hasPassthrough();
	}

	@Test
	public void anonymizeAny() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleAnyPathMaxSizeJsonFilter(size, -1, DEFAULT_ANY_PATH, FilterType.ANON);

		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(-1, DEFAULT_ANY_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
	}

	@Test
	public void pruneAny() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleAnyPathMaxSizeJsonFilter(size, -1, DEFAULT_ANY_PATH, FilterType.PRUNE);
		
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(-1, DEFAULT_ANY_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();
	}

	@Test
	public void anonymizeAnyMaxPathMatches() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleAnyPathMaxSizeJsonFilter(size, 1, "//key1", FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(1, "//key1", FilterType.ANON)).hasAnonymized("//key1").hasAnonymizeMetrics();
		
		maxSize = (size) -> new MustContrainSingleAnyPathMaxSizeJsonFilter(size, 2,  "//child1", FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(2, "//child1", FilterType.ANON)).hasAnonymized("//child1").hasAnonymizeMetrics();
		
		maxSize = (size) -> new MustContrainSingleAnyPathMaxSizeJsonFilter(size, 1, DEFAULT_ANY_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(1, DEFAULT_ANY_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
	}
	
	@Test
	public void pruneAnyMaxPathMatches() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new SingleAnyPathMaxSizeJsonFilter(size, 1, "//key3", FilterType.PRUNE);
		
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(1, "//key3", FilterType.PRUNE)).hasPruned("//key3").hasPruneMetrics();
		
		maxSize = (size) -> new MustContrainSingleAnyPathMaxSizeJsonFilter(size, 1, DEFAULT_ANY_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleAnyPathJsonFilter(1, DEFAULT_ANY_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();

	}
	
	@Test
	public void exception_returns_false() throws Exception {
		JsonFilter filter = new SingleAnyPathMaxSizeJsonFilter(-1, -1, DEFAULT_ANY_PATH, FilterType.PRUNE);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertNull(filter.process(new byte[] {}, 1, 1));
	}	

	@Test
	public void test() {
		// String string = "{\"key1\":\"aa\",\"key2\":\"abcdefghijklmnopqrstuvwxyz0123456789\"}";
		String string = "{\n"
				+ "  \"key\" : [\n"
				+ "    \"aaaaaaaaaaaaaaaaaaaa\"\n"
				+ "  ]\n"
				+ "}";
		
		System.out.println("Input size is ");
		
		int size = 42;
		
		SingleAnyPathMaxSizeJsonFilter filter = new MustContrainSingleAnyPathMaxSizeJsonFilter(size, 1, ANY_PASSTHROUGH_XPATH, FilterType.PRUNE);
		
		System.out.println("Original:");
		System.out.println(string);
		System.out.println("Filtered:");

		String filtered = filter.process(string);
		System.out.println(filtered);
		
		byte[] filteredBytes = filter.process(string.getBytes());
		System.out.println(new String(filteredBytes));
		
		System.out.println(filtered.length());

		long epochSecond = Instant.EPOCH.plus(51 * 365L, ChronoUnit.DAYS).getEpochSecond(); // 1970 + almost 51 years

		Instant.EPOCH.plus(51 * 365L, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
		
		System.out.println(epochSecond);
	}
}
