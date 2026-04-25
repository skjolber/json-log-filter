package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.Generator;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair.MaxSizeJsonFilterFunction;

public class PathMaxSizeMaxStringLengthJsonFilterTest extends DefaultJsonFilterTest {

	private static class MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter extends PathMaxSizeMaxStringLengthJsonFilter {

		public MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
			super(maxStringLength, maxSize, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
		}

		public MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String[] anonymizes, String[] prunes) {
			super(maxStringLength, maxSize, maxPathMatches, anonymizes, prunes);
		}

		@Override
		protected boolean mustConstrainMaxSize(int length) {
			return true;
		}
	};
	
	public PathMaxSizeMaxStringLengthJsonFilterTest() throws Exception {
		super();
	}

	@Test
	@ResourceLock(value = "jackson")
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new PathMaxSizeMaxStringLengthJsonFilter(128, size, -1, new String[] {"/CVE_Items/cve/CVE_data_meta"}, null));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new PathMaxSizeMaxStringLengthJsonFilter(128, size, -1, null, new String[] {"/CVE_Items/cve/CVE_data_meta"}));
		
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new PathMaxSizeMaxStringLengthJsonFilter(128, size, -1,new String[] {"/CVE_Items/impact/baseMetricV2/severity"}, null));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new PathMaxSizeMaxStringLengthJsonFilter(128, size, -1,null, new String[] {"/CVE_Items/impact/baseMetricV2/severity"}));

		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new PathMaxSizeMaxStringLengthJsonFilter(128, size, -1,new String[] {"/CVE_Items/impact/baseMetricV2/impactScore"}, null));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new PathMaxSizeMaxStringLengthJsonFilter(128, size, -1,null, new String[] {"/CVE_Items/impact/baseMetricV2/impactScore"}));
		
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new PathMaxSizeMaxStringLengthJsonFilter(128, size, -1,new String[] {"/CVE_Items/impact/baseMetricV2/obtainAllPrivilege"}, null));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new PathMaxSizeMaxStringLengthJsonFilter(128, size, -1,null, new String[] {"/CVE_Items/impact/baseMetricV2/obtainAllPrivilege"}));
	}
	
	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(128, size, -1, new String[] {"/CVE_Items/cve/CVE_data_meta"}, null));
	}
	
	@Test
	public void testDeepStructure2() throws IOException {
		validateDeepStructure( (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(128, size, -1, new String[] {DEEP_PATH}, null));
	}
	
	@Test
	public void passthrough_success() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, null, null);
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, -1, null, null)).hasPassthrough();
		
		maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, -1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasPassthrough();
	}
	
	@Test
	public void anonymize() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, new String[]{DEFAULT_PATH}, null);
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, -1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		
		maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, -1, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		
		maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, new String[]{DEEP_PATH1, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, -1, new String[]{DEEP_PATH1, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEEP_PATH1).hasAnonymizeMetrics();
	}
	
	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, new String[]{"/key1"}, null);
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, 1, new String[]{"/key1"}, null)).hasAnonymized("/key1");
		

		maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, new String[]{DEFAULT_PATH}, null);
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, 1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		
		maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, 2, new String[]{DEFAULT_PATH}, null);
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, 2, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
	}

	@Test
	public void anonymizeWildcard() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, new String[]{DEFAULT_WILDCARD_PATH}, null);
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, -1, new String[]{DEFAULT_WILDCARD_PATH}, null)).hasAnonymized(DEFAULT_WILDCARD_PATH).hasAnonymizeMetrics();
	}
	
	@Test
	public void anonymizeAny() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, new String[]{DEFAULT_ANY_PATH}, null);
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, -1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
	}

	@Test
	public void prune() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, null, new String[]{DEFAULT_PATH});
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		
		maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH});
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		
		maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, null, new String[]{DEEP_PATH3, PASSTHROUGH_XPATH});
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEEP_PATH3, PASSTHROUGH_XPATH})).hasPruned(DEEP_PATH3).hasPruneMetrics();
	}
	
	@Test
	public void pruneMaxPathMatches() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, null, new String[]{"/key3"});
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, 1, null, new String[]{"/key3"})).hasPruned("/key3").hasPruneMetrics();
		
		maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, null, new String[]{DEFAULT_PATH});
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, 1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		
		maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, 2, null, new String[]{DEFAULT_PATH});
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, 2, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
	}

	@Test
	public void pruneWildcard() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, null, new String[]{DEFAULT_WILDCARD_PATH});
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEFAULT_WILDCARD_PATH})).hasPruned(DEFAULT_WILDCARD_PATH).hasPruneMetrics();
	}
	
	@Test
	public void pruneAny() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, null, new String[]{DEFAULT_ANY_PATH});
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();
	}

	@Test
	public void maxStringLength() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, -1, null, null);
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, null, null)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH).hasMaxStringLengthMetrics();
	}
	
	@Test
	public void maxStringLengthAnonymizePrune() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, 2, new String[]{"/key1"}, new String[]{"/key3"});

		assertThat(maxSize, new PathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, 2, new String[]{"/key1"}, new String[]{"/key3"}))
			.hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH)
			.hasMaxPathMatches(2)
			.hasPruned("/key3").hasPruneMetrics()
			.hasAnonymized("/key1").hasAnonymizeMetrics();
	}

	@Test
	public void exception_returns_false() throws Exception {
		PathMaxStringLengthJsonFilter filter = new PathMaxSizeMaxStringLengthJsonFilter(-1, -1, -1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void maxStringLengthWithAnonymizeDeepPath() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(
			-1, size, -1, new String[]{DEEP_PATH1}, null);
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, -1, new String[]{DEEP_PATH1}, null))
			.hasAnonymized(DEEP_PATH1).hasAnonymizeMetrics();
	}

	@Test
	public void maxStringLengthWithAnonymizeAny() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(
			-1, size, -1, new String[]{DEFAULT_ANY_PATH}, null);
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, -1, new String[]{DEFAULT_ANY_PATH}, null))
			.hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
	}

	@Test
	public void maxStringLengthWithPruneAny() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(
			-1, size, -1, null, new String[]{DEFAULT_ANY_PATH});
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEFAULT_ANY_PATH}))
			.hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();
	}

	@Test
	public void maxStringLengthWithBothPathsAndMaxPathMatches1() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(
			DEFAULT_MAX_STRING_LENGTH, size, 2, new String[]{"/key1"}, new String[]{"/key3"});
		assertThat(maxSize, new PathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, 2, new String[]{"/key1"}, new String[]{"/key3"}))
			.hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH)
			.hasMaxPathMatches(2)
			.hasPruned("/key3").hasPruneMetrics()
			.hasAnonymized("/key1").hasAnonymizeMetrics();
	}

	@Test
	public void anonymizeObjectValue() throws Exception {
		// Triggers anonymizeSubtree when matched path key has an object/array value + maxSize active
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, new String[]{"/key2"}, null);
		assertThat(maxSize, new PathMaxSizeMaxStringLengthJsonFilter(-1, 200, -1, new String[]{"/key2"}, null)).hasAnonymized("/key2");

		maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, null, new String[]{"/key2"});
		assertThat(maxSize, new PathMaxSizeMaxStringLengthJsonFilter(-1, 200, -1, null, new String[]{"/key2"})).hasPruned("/key2");

		// Triggers skipObjectMaxStringLength for sibling object + anonymizeSubtree for object value + maxSize
		maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, -1, new String[]{"/key1"}, null);
		assertThat(maxSize, new PathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, 200, -1, new String[]{"/key1"}, null)).hasAnonymized("/key1");
	}

	@Test
	public void testGrowSquareBrackets() throws Exception {
		// 35 levels of nesting forces the filter's bracket-tracking to grow. An any-path filter is used so all nested objects are traversed rather than skipped.
		byte[] jsonBytes = Generator.generateDeepObjectStructure(35, "longvaluelongvaluelongvalue", false);
		String json = new String(jsonBytes, StandardCharsets.UTF_8);

		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(5, json.length() + 100, -1, new String[]{"//targetKey"}, null);

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(512);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput));
	}

	@Test
	public void testNonStringValuesAtMatchedPath() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, new String[]{"/boolKey"}, null);
		assertThat(maxSize, new PathMaxSizeMaxStringLengthJsonFilter(-1, -1, -1, new String[]{"/boolKey"}, null)).hasAnonymized("/boolKey").hasAnonymizeMetrics();

		MaxSizeJsonFilterFunction maxSizeNull = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, new String[]{"/nullKey"}, null);
		assertThat(maxSizeNull, new PathMaxSizeMaxStringLengthJsonFilter(-1, -1, -1, new String[]{"/nullKey"}, null)).hasAnonymized("/nullKey").hasAnonymizeMetrics();

		MaxSizeJsonFilterFunction maxSizeFalse = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, new String[]{"/falseKey"}, null);
		assertThat(maxSizeFalse, new PathMaxSizeMaxStringLengthJsonFilter(-1, -1, -1, new String[]{"/falseKey"}, null)).hasAnonymized("/falseKey").hasAnonymizeMetrics();

		MaxSizeJsonFilterFunction maxSizeNum = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, new String[]{"/numKey"}, null);
		assertThat(maxSizeNum, new PathMaxSizeMaxStringLengthJsonFilter(-1, -1, -1, new String[]{"/numKey"}, null)).hasAnonymized("/numKey").hasAnonymizeMetrics();

		MaxSizeJsonFilterFunction maxSizePruneBool = (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, null, new String[]{"/boolKey"});
		assertThat(maxSizePruneBool, new PathMaxSizeMaxStringLengthJsonFilter(-1, -1, -1, null, new String[]{"/boolKey"})).hasPruned("/boolKey").hasPruneMetrics();
	}

	@Test
	public void testPruneMessageDoesNotFit() throws Exception {
		// maxSize=10 → after '{' maxSizeLimit=9. Path /k matches. nextOffset=5.
		// Default pruneMessage is ~13 chars. 5+13=18 > 9 → break loop.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 10, -1, null, new String[]{"/k"});
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/pathShortKey/objectKLongNext.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testAnonMessageDoesNotFit() throws Exception {
		// Same as above but with anonymize path
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 10, -1, new String[]{"/k"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/pathShortKey/objectKLongNext.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testPruneObjectValue() throws Exception {
		// Path match where value is an object/array - tests the object-skip path in prune
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 200, -1, null, new String[]{"/k"});
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/pathShortKey/objectKNestedOther.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testAnonObjectValue() throws Exception {
		// Anonymize an object value
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 200, -1, new String[]{"/k"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/pathShortKey/objectKNestedOther.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testPruneWithRemoveLastFilter() throws Exception {
		// After pruning, if the cursor moves past the size limit, the filter removes the partial entry to stay within bounds.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 15, -1, null, new String[]{"/k"}, "X", "X", "X");
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/pathShortKey/objectKLongN.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testAnonWithRemoveLastFilter() throws Exception {
		// After anonymizing, if the cursor moves past the size limit, the filter removes the partial entry.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 15, -1, new String[]{"/k"}, null, "X", "X", "X");
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/pathShortKey/objectKLongN.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testPruneQuotedValue() throws Exception {
		// Path match for a string value (quoted) that gets pruned
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 200, -1, null, new String[]{"/k"});
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/pathShortKey/objectKValue.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testAnonQuotedValue() throws Exception {
		// Anonymize a string value
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 200, -1, new String[]{"/k"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/pathShortKey/objectKValue.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testExceptionInRangesReturnsFalse() throws Exception {
		// An invalid input offset causes the filter to return false rather than throw.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 100, -1, new String[]{"/k"}, null);
		assertFalse(filter.process(new char[]{}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[]{}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void testPathMatchesExhausted() throws Exception {
		// After the single allowed path match is consumed and the size limit covers the whole document, the filter uses the unconstrained multi-path path.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 200, 1, null, new String[]{"/k"});
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/pathShortKey/objectKDuplicateFull.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testPathMatchesExhaustedWithMaxSize() throws Exception {
		// After the single allowed match is consumed with a tight size limit, the filter switches to the size-constrained path.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 15, 1, null, new String[]{"/k"});
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/pathShortKey/objectKVOtherVeryLong.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testPruneRemoveLastFilterLargePruneMessage() throws Exception {
		// When the prune message is much larger than the value being replaced, the added size may push the cursor past the size limit, and the filter removes the partial entry.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 20, -1, null, new String[]{"/k"},
				"X".repeat(25), "X".repeat(25), "X".repeat(25));
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/pathShortKey/objectKVN.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testAnonRemoveLastFilterLargeAnonMessage() throws Exception {
		// When the anonymization message is larger than the value being replaced, the added size may push the cursor past the size limit, and the filter removes the partial entry.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 20, -1, new String[]{"/k"}, null,
				"X".repeat(25), "X".repeat(25), "X".repeat(25));
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/pathShortKey/objectKVN.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testStringLengthBreakLoop() throws Exception {
		// When the string-length limit added to the current position exceeds the size limit, the filter stops and truncates.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(3, 8, -1, new String[]{"/other"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/pathShortKey/objectKLongOtherShort.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testStringLengthRemoveLastFilter() throws Exception {
		// Truncating a non-matched long value with a message larger than the original causes the cursor to move past the size limit, so the filter removes the partial entry.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(3, 50, -1, new String[]{"/other"}, null,
				"X".repeat(25), "X".repeat(25), "X".repeat(25));
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/pathShortKey/objectKVvvvOther.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testDeepArrayGrowSquareBrackets() throws Exception {
		// 34 nested arrays force the bracket-tracking array to grow while traversing the array path.
		StringBuilder deepJson = new StringBuilder();
		deepJson.append("{\"k\":");
		for (int i = 0; i < 34; i++) {
			deepJson.append("[");
		}
		deepJson.append("1");
		for (int i = 0; i < 34; i++) {
			deepJson.append("]");
		}
		deepJson.append(",\"other\":\"v\"}");
		String json = deepJson.toString();

		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 5000, -1, new String[]{"/other"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testAnonymizePathMatchesExhausted() throws Exception {
		// Anonymize case: pathMatches=1, exhausted after first match
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 200, 1, new String[]{"/k"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/pathShortKey/objectKDuplicateFull.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testAnonObjectValueRemoveLastFilter() throws Exception {
		// When anonymizing an object value and the anonymized subtree is larger than the remaining allowed size, the filter truncates cleanly.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 25, -1, new String[]{"/k"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/pathShortKey/objectKObjectOther.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testStringLengthRemoveLastFilterViaLongValue() throws Exception {
		// Truncating a long non-path string value with a tight size limit may move the cursor past the limit, causing the filter to remove the partial entry.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(5, 24, -1, new String[]{"/other"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/pathShortKey/objectNoMatchLong.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testSkipObjectOrArrayWithLongKeys() throws Exception {
		// Verifies that long field names inside a non-matched nested object are handled correctly when skipping and truncating.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(5, 300, -1, new String[]{"/a"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/deepPath/objectABCLongkeys.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testSkipObjectWithLongKeyAndWhitespaceBeforeColon() throws Exception {
		// skipObjectOrArrayMaxSizeMaxStringLength: long key with whitespace before colon
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(5, 300, -1, new String[]{"/a"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/deepPath/objectABCLongkeysSpaced.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testSkipObjectWithLongValueBreakLoop() throws Exception {
		// When a string inside a skipped nested object hits both the string-length limit and the size limit simultaneously, the filter stops cleanly.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(5, 35, -1, new String[]{"/a"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/deepPath/objectABCKeyLongValue.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testSkipDeepObjectGrow() throws Exception {
		// Deep nesting inside a non-matched object forces the bracket-tracking array to grow during the skip-and-truncate path.
		StringBuilder deepValue = new StringBuilder();
		for (int i = 0; i < 35; i++) {
			deepValue.append("{\"n").append(String.format("%02d", i)).append("\":");
		}
		deepValue.append("1");
		for (int i = 0; i < 35; i++) {
			deepValue.append("}");
		}
		String json = "{\"a\":\"v\",\"b\":{\"c\":" + deepValue + "}}";
		int maxSize = json.length(); // match exactly to avoid crash

		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, maxSize, -1, new String[]{"/a"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testAnonDeepObjectGrow() throws Exception {
		// The anonymized value contains 35 levels of nesting, forcing the filter's bracket-tracking to grow during subtree anonymization.
		StringBuilder deepValue = new StringBuilder();
		for (int i = 0; i < 35; i++) {
			deepValue.append("{\"n").append(String.format("%02d", i)).append("\":");
		}
		deepValue.append("1");
		for (int i = 0; i < 35; i++) {
			deepValue.append("}");
		}
		String json = "{\"k\":" + deepValue + ",\"other\":\"v\"}";
		int maxSize = json.length();

		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, maxSize, -1, new String[]{"/k"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testByteStringWithEscapedQuote() throws Exception {
		// A string value containing an escaped quote character is processed correctly in the byte path.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 5000, -1, new String[]{"/k"}, null);
		// String with escaped quote: "He said \"hello\" to everyone in the room"
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/pathShortKey/objectKQuotedValue.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		// Byte version triggers ByteArrayRangesFilter.scanEscapedValue (line 487)
		// and the word-at-a-time scan if string after escaped quote is long enough (lines 529-530)
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testByteSkipSubtreeWithFalseValue() throws Exception {
		// A false value inside a non-matched nested object is handled correctly in the byte path.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 5000, -1, new String[]{"/a"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/deepPath/objectABFlags.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testPathMatchesExhaustedWithMaxSizeLargerThanReadLimit() throws Exception {
		// After the single allowed match is consumed, if pruning increases the size limit to cover the document, the filter transitions to unconstrained processing.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 30, 1, null, new String[]{"/k"});
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/pathShortKey/objectKLongXOther.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testMaxSizeLargerThanReadLimitAfterMatch() throws Exception {
		// After a match with remaining budget, if pruning causes the size limit to cover the rest of the document, the filter transitions to unconstrained processing.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 30, 2, null, new String[]{"/k"});
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/pathShortKey/objectKLongXOther.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testKeyWithWhitespaceBeforeColonInSkippedSubtree() throws Exception {
		// A key with whitespace before its colon inside a non-matched nested object is handled correctly.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 5000, -1, new String[]{"/a"}, null);
		// "b"'s value has key "c" with whitespace before colon: "c"   :"v"
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/deepPath/objectABCSpaced.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testSkipObjectMaxSizeOverflow() throws Exception {
		// Truncating a long string inside a non-matched nested object can bring the remaining size limit up to the document length, causing the filter to transition to unconstrained processing.
		String longVal = "a".repeat(200);
		String json = "{\"a\":{\"b\":{\"inner\":\"" + longVal + "\"}},\"path\":\"v\"}";
		int maxSize = json.length() - 4; // mustConstrainMaxSize returns true
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(5, maxSize, -1, new String[]{"/path"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testAnonSubtreeScalarCannotFit() throws Exception {
		// When the anonymization message for a scalar value inside a matched subtree does not fit in the remaining size budget, the filter truncates cleanly.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 17, -1, new String[]{"/key"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/cornercases/fullPath/objectKeyValTrue.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}


}
