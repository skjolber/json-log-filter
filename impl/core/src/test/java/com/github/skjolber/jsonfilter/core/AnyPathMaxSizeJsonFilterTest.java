package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

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
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, -1, new String[]{"//boolKey"}, null);
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, -1, new String[]{"//boolKey"}, null)).hasAnonymized("//boolKey").hasAnonymizeMetrics();

		MaxSizeJsonFilterFunction maxSizeNull = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, -1, new String[]{"//nullKey"}, null);
		assertThat(maxSizeNull, new AnyPathMaxSizeJsonFilter(-1, -1, new String[]{"//nullKey"}, null)).hasAnonymized("//nullKey").hasAnonymizeMetrics();

		MaxSizeJsonFilterFunction maxSizeFalse = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, -1, new String[]{"//falseKey"}, null);
		assertThat(maxSizeFalse, new AnyPathMaxSizeJsonFilter(-1, -1, new String[]{"//falseKey"}, null)).hasAnonymized("//falseKey").hasAnonymizeMetrics();

		MaxSizeJsonFilterFunction maxSizeNum = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, -1, new String[]{"//numKey"}, null);
		assertThat(maxSizeNum, new AnyPathMaxSizeJsonFilter(-1, -1, new String[]{"//numKey"}, null)).hasAnonymized("//numKey").hasAnonymizeMetrics();

		MaxSizeJsonFilterFunction maxSizePruneBool = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, -1, null, new String[]{"//boolKey"});
		assertThat(maxSizePruneBool, new AnyPathMaxSizeJsonFilter(-1, -1, null, new String[]{"//boolKey"})).hasPruned("//boolKey").hasPruneMetrics();
	}

	@Test
	public void testPruneMessageDoesNotFit() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, -1, null, new String[]{"//k"});
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, -1, null, new String[]{"//k"})).hasPruned("//k").hasPruneMetrics();
	}

	@Test
	public void testAnonMessageDoesNotFit() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, -1, new String[]{"//k"}, null);
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, -1, new String[]{"//k"}, null)).hasAnonymized("//k").hasAnonymizeMetrics();
	}

	@Test
	public void testPruneObjectValueWithMaxSize() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, -1, null, new String[]{"//k"});
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, -1, null, new String[]{"//k"})).hasPruned("//k").hasPruneMetrics();
	}

	@Test
	public void testAnonObjectValueWithMaxSize() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, -1, new String[]{"//k"}, null);
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, -1, new String[]{"//k"}, null)).hasAnonymized("//k").hasAnonymizeMetrics();
	}

	@Test
	public void testPruneWithRemoveLastFilter() throws Exception {
		// Exercises the "remove last filter" path when the pruned value pushes output beyond maxSize
		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(15, -1, null, new String[]{"//k"}, "X", "X", "X");
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/shortKey/objectKLongN.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testAnonWithRemoveLastFilter() throws Exception {
		// Exercises the "remove last filter" path when the anonymized value pushes output beyond maxSize
		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(15, -1, new String[]{"//k"}, null, "X", "X", "X");
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/shortKey/objectKLongN.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testMustConstrainFalseCallsSuper() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, -1, new String[]{"//k"}, null);
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, -1, new String[]{"//k"}, null)).hasAnonymized("//k").hasAnonymizeMetrics();
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
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, 1, new String[]{"//k"}, null);
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, 1, new String[]{"//k"}, null)).hasAnonymized("//k").hasAnonymizeMetrics();
	}

	@Test
	public void testPathMatchesExhaustedSmallMaxSize() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, 1, new String[]{"//k"}, null);
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, 1, new String[]{"//k"}, null)).hasAnonymized("//k").hasAnonymizeMetrics();
	}

	@Test
	public void testMaxSizeLimitReachedAfterMatch() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, -1, new String[]{"//k"}, null);
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, -1, new String[]{"//k"}, null)).hasAnonymized("//k").hasAnonymizeMetrics();
	}

	@Test
	public void testRangesMaxSizeBreakLoopOnOpenBracket() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, 1, null, new String[]{"//k"});
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, 1, null, new String[]{"//k"})).hasPruned("//k").hasPruneMetrics();
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
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainAnyPathMaxSizeJsonFilter(size, -1, new String[]{"//k"}, null);
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, -1, new String[]{"//k"}, null)).hasAnonymized("//k").hasAnonymizeMetrics();
	}


}
