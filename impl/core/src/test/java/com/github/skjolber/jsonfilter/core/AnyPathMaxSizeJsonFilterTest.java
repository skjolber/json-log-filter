package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.Generator;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair.MaxSizeJsonFilterFunction;

public class AnyPathMaxSizeJsonFilterTest extends DefaultJsonFilterTest {
	
	private static class MustContrainAnyPathMaxSizeJsonFilter extends AnyPathMaxSizeJsonFilter {

		public MustContrainAnyPathMaxSizeJsonFilter(int maxSize, int maxPathMatches, String[] anonymizes, String[] prunes,
				String pruneMessage, String anonymizeMessage, String truncateMessage) {
			super(maxSize, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
		}

		public MustContrainAnyPathMaxSizeJsonFilter(int maxSize, int maxPathMatches, String[] anonymizes, String[] prunes) {
			super(maxSize, maxPathMatches, anonymizes, prunes);
		}

		@Override
		protected boolean mustConstrainMaxSize(int length) {
			return true;
		}
	};
	
	public AnyPathMaxSizeJsonFilterTest() throws Exception {
		super();
	}

	@Test
	@ResourceLock(value = "jackson")
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, -1, null, new String[] {"//CVE_data_meta"}));
	}
	
	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, -1, new String[] {ANY_PASSTHROUGH_XPATH}, null));
	}
	
	@Test
	public void passthrough_success() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, -1, null, null);
		
		maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, -1, new String[]{ANY_PASSTHROUGH_XPATH}, new String[]{ANY_PASSTHROUGH_XPATH});
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, -1, new String[]{ANY_PASSTHROUGH_XPATH}, new String[]{ANY_PASSTHROUGH_XPATH})).hasPassthrough();
	}
	
	@Test
	public void anonymize() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, -1, new String[]{DEFAULT_ANY_PATH}, null);
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, -1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
	}
	
	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, 1, new String[]{DEFAULT_ANY_PATH}, null);
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, 1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
		
		maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, 2, new String[]{DEFAULT_ANY_PATH}, null);
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, 2, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
	}

	@Test
	public void prune() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, -1, null, new String[]{DEFAULT_ANY_PATH});
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, -1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();
	}
	
	@Test
	public void pruneMaxPathMatches() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, 1, null, new String[]{DEFAULT_ANY_PATH});
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, 1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();
		
		maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, 2, null, new String[]{DEFAULT_ANY_PATH});
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, 2, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();
	}

	@Test
	public void exception_returns_false() throws Exception {
		AnyPathMaxSizeJsonFilter filter = new AnyPathMaxSizeJsonFilter(-1, -1, new String[]{DEFAULT_ANY_PATH}, new String[]{DEFAULT_ANY_PATH});
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void anonymizeObjectValue() throws Exception {
		// Triggers anonymizeSubtree when matched any-path key has an object/array value + maxSize active
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, -1, new String[]{"//key2"}, null);
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(200, -1, new String[]{"//key2"}, null)).hasAnonymized("//key2");

		maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, -1, null, new String[]{"//key2"});
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(200, -1, null, new String[]{"//key2"})).hasPruned("//key2");
	}

	@Test
	public void testGrowSquareBrackets() throws Exception {
		// 35 levels of nesting forces the filter's bracket-tracking array to grow beyond its initial capacity.
		// An any-path filter is used so all nested objects are traversed rather than skipped.
		byte[] jsonBytes = Generator.generateDeepObjectStructure(35, "x".repeat(500), false);
		String json = new String(jsonBytes, StandardCharsets.UTF_8);

		MustContrainAnyPathMaxSizeJsonFilter filter = new MustContrainAnyPathMaxSizeJsonFilter(400, -1, new String[]{"//targetKey"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));

		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testNonStringValuesWithMaxSize() throws Exception {
		// Test rangesAnyPathMaxSize with true/false/null/number values
		MustContrainAnyPathMaxSizeJsonFilter filter;

		// true value
		filter = new MustContrainAnyPathMaxSizeJsonFilter(200, -1, new String[]{"//boolKey"}, null);
		String jsonTrue = "{\"boolKey\":true,\"other\":\"data\"}";
		assertNotNull(filter.process(jsonTrue.toCharArray(), 0, jsonTrue.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonTrue.getBytes(StandardCharsets.UTF_8)));

		// null value
		filter = new MustContrainAnyPathMaxSizeJsonFilter(200, -1, new String[]{"//nullKey"}, null);
		String jsonNull = "{\"nullKey\":null,\"other\":\"data\"}";
		assertNotNull(filter.process(jsonNull.toCharArray(), 0, jsonNull.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonNull.getBytes(StandardCharsets.UTF_8)));

		// false value
		filter = new MustContrainAnyPathMaxSizeJsonFilter(200, -1, new String[]{"//falseKey"}, null);
		String jsonFalse = "{\"falseKey\":false,\"other\":\"data\"}";
		assertNotNull(filter.process(jsonFalse.toCharArray(), 0, jsonFalse.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonFalse.getBytes(StandardCharsets.UTF_8)));

		// number value
		filter = new MustContrainAnyPathMaxSizeJsonFilter(200, -1, new String[]{"//numKey"}, null);
		String jsonNum = "{\"numKey\":12345,\"other\":\"data\"}";
		assertNotNull(filter.process(jsonNum.toCharArray(), 0, jsonNum.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonNum.getBytes(StandardCharsets.UTF_8)));

		// prune with non-string values
		filter = new MustContrainAnyPathMaxSizeJsonFilter(200, -1, null, new String[]{"//boolKey"});
		assertNotNull(filter.process(jsonTrue.toCharArray(), 0, jsonTrue.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonTrue.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testPruneMessageDoesNotFit() throws Exception {
		// When the prune message is larger than the remaining allowed size, the filter stops processing and truncates.
		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(10, -1, null, new String[]{"//k"});
		String json = "{\"k\":\"longlonglongvalue\",\"next\":\"more\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testAnonMessageDoesNotFit() throws Exception {
		// Same as above but with anonymize path
		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(10, -1, new String[]{"//k"}, null);
		String json = "{\"k\":\"longlonglongvalue\",\"next\":\"more\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testPruneObjectValueWithMaxSize() throws Exception {
		// Prune where value is an object - tests skipObjectOrArray path
		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(200, -1, null, new String[]{"//k"});
		String json = "{\"k\":{\"nested\":\"value\"},\"other\":\"data\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testAnonObjectValueWithMaxSize() throws Exception {
		// Anonymize an object value
		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(200, -1, new String[]{"//k"}, null);
		String json = "{\"k\":{\"nested\":\"value\"},\"other\":\"data\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testPruneWithRemoveLastFilter() throws Exception {
		// After pruning, if the result exceeds the size limit, the filter removes the partial entry to stay within bounds.
		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(
				15, -1, null, new String[]{"//k"},
				"X", "X", "X");
		String json = "{\"k\":\"longlonglong\",\"n\":\"v\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testAnonWithRemoveLastFilter() throws Exception {
		// After anonymizing, if the cursor moves past the size limit, the filter removes the partial entry.
		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(
				15, -1, new String[]{"//k"}, null,
				"X", "X", "X");
		String json = "{\"k\":\"longlonglong\",\"n\":\"v\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testMustConstrainFalseCallsSuper() throws Exception {
		// When the document is much smaller than maxSize, the filter delegates to the base path-filter without size constraints.
		AnyPathMaxSizeJsonFilter filter = new AnyPathMaxSizeJsonFilter(100000, -1, new String[]{"//k"}, null);
		String json = "{\"k\":\"value\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testExceptionInRangesReturnsFalse() throws Exception {
		// An invalid input offset causes the filter to return false rather than throw.
		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(100, -1, new String[]{"//k"}, null);
		assertFalse(filter.process(new char[]{}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[]{}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void testPathMatchesExhaustedLargeMaxSize() throws Exception {
		// When only one path match is allowed and the size limit covers the whole document, the filter switches to the unconstrained path after the match.
		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(500, 1, new String[]{"//k"}, null);
		String json = "{\"k\":\"v1\",\"k\":\"v2\",\"other\":\"data\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testPathMatchesExhaustedSmallMaxSize() throws Exception {
		// When only one match is allowed and the size limit is tight, the filter switches to the size-constrained path after the match.
		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(15, 1, new String[]{"//k"}, null);
		String json = "{\"k\":\"v\",\"other\":\"data and more data\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testMaxSizeLimitReachedAfterMatch() throws Exception {
		// After the path match, removing the value makes the size limit cover the rest of the document, so the filter continues without the size constraint.
		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(500, -1, new String[]{"//k"}, null);
		// large maxSize so after matching and removing, maxSizeLimit >= maxReadLimit
		String json = "{\"k\":\"longlonglongvalue\",\"other\":\"data\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testRangesMaxSizeBreakLoopOnOpenBracket() throws Exception {
		// A nested bracket that takes the position past the size limit causes the filter to stop and truncate.
		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(23, 1, null, new String[]{"//k"});
		String json = "{\"k\":\"verylongvalueXX\",\"n\":[1]}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testRangesMaxSizeGrowSquareBrackets() throws Exception {
		// Matching an early key with pathMatches=1 hands off to rangesMaxSize, which then traverses
		// 35 levels of nesting in the remainder of the document, forcing bracket-tracking to grow.
		byte[] jsonBytes = Generator.generateObjectWithShallowKeyAndDeepValue(35, "k", "v", "deep", false);
		String json = new String(jsonBytes, StandardCharsets.UTF_8);

		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(json.length() + 100, 1, new String[]{"//k"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testAnonBreakLoopMessageTooLarge() throws Exception {
		// When the anonymization replacement is too large to fit in the remaining allowed size, the filter stops and truncates.
		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(8, -1, new String[]{"//k"}, null);
		String json = "{\"k\":\"val\",\"other\":\"v\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

}
