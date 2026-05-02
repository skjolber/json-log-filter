package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
		validateDeepStructure( (size) -> new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(128, size, -1, new String[] {"//f2"}, null));
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
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/shortKey/objectKLongNext.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		assertEquals("{}", charOutput.toString());
		byte[] byteResult = filter.process(jsonBytes);
		assertNotNull(byteResult);
		assertEquals("{}", new String(byteResult, StandardCharsets.UTF_8));
	}

	@Test
	public void testAnonMessageDoesNotFit() throws Exception {
		// Same as above but with anonymize path
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 10, -1, new String[]{"/k"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/shortKey/objectKLongNext.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		assertEquals("{\"k\":\"*\"}", charOutput.toString());
		byte[] byteResult = filter.process(jsonBytes);
		assertNotNull(byteResult);
		assertEquals("{\"k\":\"*\"}", new String(byteResult, StandardCharsets.UTF_8));
	}


	@Test
	public void testPruneWithRemoveLastFilter() throws Exception {
		// After pruning, if the cursor moves past the size limit, the filter removes the partial entry to stay within bounds.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 15, -1, null, new String[]{"/k"}, "X", "X", "X");
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/shortKey/objectKLongN.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		assertEquals("{\"k\":X,\"n\":\"v\"}", charOutput.toString());
		byte[] byteResult = filter.process(jsonBytes);
		assertNotNull(byteResult);
		assertEquals("{\"k\":X,\"n\":\"v\"}", new String(byteResult, StandardCharsets.UTF_8));
	}

	@Test
	public void testAnonWithRemoveLastFilter() throws Exception {
		// After anonymizing, if the cursor moves past the size limit, the filter removes the partial entry.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 15, -1, new String[]{"/k"}, null, "X", "X", "X");
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/shortKey/objectKLongN.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		assertEquals("{\"k\":X,\"n\":\"v\"}", charOutput.toString());
		byte[] byteResult = filter.process(jsonBytes);
		assertNotNull(byteResult);
		assertEquals("{\"k\":X,\"n\":\"v\"}", new String(byteResult, StandardCharsets.UTF_8));
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
	public void testPathMatchesExhaustedWithMaxSize() throws Exception {
		// After the single allowed match is consumed with a tight size limit, the filter switches to the size-constrained path.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 15, 1, null, new String[]{"/k"});
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/shortKey/objectKVOtherVeryLong.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		assertEquals("{}", charOutput.toString());
		byte[] byteResult2 = filter.process(jsonBytes);
		assertNotNull(byteResult2);
		assertEquals("{}", new String(byteResult2, StandardCharsets.UTF_8));
	}

	@Test
	public void testPruneRemoveLastFilterLargePruneMessage() throws Exception {
		// When the prune message is much larger than the value being replaced, the added size may push the cursor past the size limit, and the filter removes the partial entry.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 20, -1, null, new String[]{"/k"},
				"X".repeat(25), "X".repeat(25), "X".repeat(25));
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/shortKey/objectKVN.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		assertEquals("{}", charOutput.toString());
		byte[] byteResult = filter.process(jsonBytes);
		assertNotNull(byteResult);
		assertEquals("{}", new String(byteResult, StandardCharsets.UTF_8));
	}

	@Test
	public void testAnonRemoveLastFilterLargeAnonMessage() throws Exception {
		// When the anonymization message is larger than the value being replaced, the added size may push the cursor past the size limit, and the filter removes the partial entry.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 20, -1, new String[]{"/k"}, null,
				"X".repeat(25), "X".repeat(25), "X".repeat(25));
		byte[] jsonBytes2 = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/shortKey/objectKVN.json"));
		String json2 = new String(jsonBytes2, StandardCharsets.UTF_8);
		StringBuilder charOutput2 = new StringBuilder();
		assertTrue(filter.process(json2.toCharArray(), 0, json2.length(), charOutput2));
		assertEquals("{}", charOutput2.toString());
		byte[] byteResult2 = filter.process(jsonBytes2);
		assertNotNull(byteResult2);
		assertEquals("{}", new String(byteResult2, StandardCharsets.UTF_8));
	}

	@Test
	public void testStringLengthBreakLoop() throws Exception {
		// When the string-length limit added to the current position exceeds the size limit, the filter stops and truncates.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(3, 8, -1, new String[]{"/other"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/shortKey/objectKLongOtherShort.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		assertEquals("{}", charOutput.toString());
		byte[] byteResult = filter.process(jsonBytes);
		assertNotNull(byteResult);
		assertEquals("{}", new String(byteResult, StandardCharsets.UTF_8));
	}

	@Test
	public void testAnonObjectValueRemoveLastFilter() throws Exception {
		// When anonymizing an object value and the anonymized subtree is larger than the remaining allowed size, the filter truncates cleanly.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 25, -1, new String[]{"/k"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/shortKey/objectKObjectOther.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		assertEquals("{\"k\":{\"a\":\"*\",\"c\":\"*\"}}", charOutput.toString());
		byte[] byteResult = filter.process(jsonBytes);
		assertNotNull(byteResult);
		assertEquals("{\"k\":{\"a\":\"*\",\"c\":\"*\"}}", new String(byteResult, StandardCharsets.UTF_8));
	}

	@Test
	public void testStringLengthRemoveLastFilterViaLongValue() throws Exception {
		// Truncating a long non-path string value with a tight size limit may move the cursor past the limit, causing the filter to remove the partial entry.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(5, 24, -1, new String[]{"/other"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/shortKey/objectNoMatchLong.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		assertEquals("{}", charOutput.toString());
		byte[] byteResult2 = filter.process(jsonBytes);
		assertNotNull(byteResult2);
		assertEquals("{}", new String(byteResult2, StandardCharsets.UTF_8));
	}

	@Test
	public void testSkipObjectWithLongValueBreakLoop() throws Exception {
		// When a string inside a skipped nested object hits both the string-length limit and the size limit simultaneously, the filter stops cleanly.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(5, 35, -1, new String[]{"/a"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/nestedObject/deepPath/objectABCKeyLongValue.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		assertEquals("{\"a\":\"*\",\"b\":{\"c\":{}}}", charOutput.toString());
		byte[] byteResult = filter.process(jsonBytes);
		assertNotNull(byteResult);
		assertEquals("{\"a\":\"*\",\"b\":{\"c\":{}}}", new String(byteResult, StandardCharsets.UTF_8));
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
	public void testPathMatchesExhaustedWithMaxSizeLargerThanReadLimit() throws Exception {
		// After the single allowed match is consumed, if pruning increases the size limit to cover the document, the filter transitions to unconstrained processing.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 30, 1, null, new String[]{"/k"});
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/shortKey/objectKLongXOther.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		assertEquals("{\"k\":\"[removed]\",\"other\":\"v\"}", charOutput.toString());
		byte[] byteResult2 = filter.process(jsonBytes);
		assertNotNull(byteResult2);
		assertEquals("{\"k\":\"[removed]\",\"other\":\"v\"}", new String(byteResult2, StandardCharsets.UTF_8));
	}

	@Test
	public void testMaxSizeLargerThanReadLimitAfterMatch() throws Exception {
		// After a match with remaining budget, if pruning causes the size limit to cover the rest of the document, the filter transitions to unconstrained processing.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 30, 2, null, new String[]{"/k"});
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/shortKey/objectKLongXOther.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		StringBuilder charOutput3 = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput3));
		assertEquals("{\"k\":\"[removed]\",\"other\":\"v\"}", charOutput3.toString());
		byte[] byteResult3 = filter.process(jsonBytes);
		assertNotNull(byteResult3);
		assertEquals("{\"k\":\"[removed]\",\"other\":\"v\"}", new String(byteResult3, StandardCharsets.UTF_8));
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
	public void testMainLoopGrowOnObject() throws Exception {
		// Processing 35 nested objects in the main loop forces the bracket-tracking array to grow (lines 64/376).
		// anyPathFilters is non-null so the skip condition is never triggered.
		byte[] jsonBytes = Generator.generateDeepObjectStructure(35, "value", false);
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, json.length(), -1, new String[]{"//nonexistent"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testMainLoopGrowOnArray() throws Exception {
		// Processing 35 nested arrays in the main loop forces the bracket-tracking array to grow (lines 109/421).
		// anyPathFilters is non-null so the skip condition is never triggered.
		byte[] jsonBytes = Generator.generateObjectWithDeepArrayValue(35, "arr", "other", "v", false);
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, json.length(), -1, new String[]{"//nonexistent"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testStringLengthTransitionToPathOnlyMode() throws Exception {
		// Truncating a long non-path string value can expand the size limit to equal the document length,
		// causing the filter to transition to unconstrained path processing (lines 172/174 and 485/486).
		// JSON: {"k":"AAAAAAAAAAAAAAAAAAA","path":"v"} = 38 chars, maxStringLength=5 (internal=7),
		// maxSize=33: after truncation saving=6, maxSizeLimit reaches 38=maxReadLimit.
		String json = "{\"k\":\"AAAAAAAAAAAAAAAAAAA\",\"path\":\"v\"}";
		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(5, 33, -1, new String[]{"/path"}, null);
		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		assertEquals("{\"k\":\"AAAAA... + 14\",\"path\":\"*\"}", charOutput.toString());
		byte[] byteResult = filter.process(jsonBytes);
		assertNotNull(byteResult);
		assertEquals("{\"k\":\"AAAAA... + 14\",\"path\":\"*\"}", new String(byteResult, StandardCharsets.UTF_8));
	}

	@Test
	public void testAnonSubtreeScalarCannotFit() throws Exception {
		// When the anonymization message for a scalar value inside a matched subtree does not fit in the remaining size budget, the filter truncates cleanly.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 17, -1, new String[]{"/key"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/shortKey/objectKeyNestedBool.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		assertEquals("{\"key\":{}}", charOutput.toString());
		byte[] byteResult = filter.process(jsonBytes);
		assertNotNull(byteResult);
		assertEquals("{\"key\":{}}", new String(byteResult, StandardCharsets.UTF_8));
	}


}
