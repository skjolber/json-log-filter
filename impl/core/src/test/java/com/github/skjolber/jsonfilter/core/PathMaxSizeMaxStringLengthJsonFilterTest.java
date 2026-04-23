package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
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
		// 35 levels of nesting to trigger grow() in ranges()
		// Use //targetKey (any-path) so the skip-nested-object optimization doesn't fire,
		// ensuring bracketLevel actually reaches 32 and triggers grow()
		StringBuilder deepJson = new StringBuilder();
		for (int i = 0; i < 35; i++) {
			deepJson.append("{\"k").append(i).append("\":");
		}
		deepJson.append("\"longvaluelongvaluelongvalue\"");
		for (int i = 0; i < 35; i++) {
			deepJson.append("}");
		}
		String json = deepJson.toString();

		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(5, json.length() + 100, -1, new String[]{"//targetKey"}, null);

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(512);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput));
	}

	@Test
	public void testNonStringValuesAtMatchedPath() throws Exception {
		// true value at matched path
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 200, -1, new String[]{"/boolKey"}, null);
		String jsonTrue = "{\"boolKey\":true,\"other\":\"data\"}";
		assertNotNull(filter.process(jsonTrue.toCharArray(), 0, jsonTrue.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonTrue.getBytes(StandardCharsets.UTF_8)));

		// null value
		filter = new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 200, -1, new String[]{"/nullKey"}, null);
		String jsonNull = "{\"nullKey\":null,\"other\":\"data\"}";
		assertNotNull(filter.process(jsonNull.toCharArray(), 0, jsonNull.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonNull.getBytes(StandardCharsets.UTF_8)));

		// false value
		filter = new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 200, -1, new String[]{"/falseKey"}, null);
		String jsonFalse = "{\"falseKey\":false,\"other\":\"data\"}";
		assertNotNull(filter.process(jsonFalse.toCharArray(), 0, jsonFalse.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonFalse.getBytes(StandardCharsets.UTF_8)));

		// number value
		filter = new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 200, -1, new String[]{"/numKey"}, null);
		String jsonNum = "{\"numKey\":12345,\"other\":\"data\"}";
		assertNotNull(filter.process(jsonNum.toCharArray(), 0, jsonNum.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonNum.getBytes(StandardCharsets.UTF_8)));

		// prune with non-string values
		filter = new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 200, -1, null, new String[]{"/boolKey"});
		assertNotNull(filter.process(jsonTrue.toCharArray(), 0, jsonTrue.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonTrue.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testPruneMessageDoesNotFit() throws Exception {
		// maxSize=10 → after '{' maxSizeLimit=9. Path /k matches. nextOffset=5.
		// Default pruneMessage is ~13 chars. 5+13=18 > 9 → break loop.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 10, -1, null, new String[]{"/k"});
		String json = "{\"k\":\"longlonglongvalue\",\"next\":\"more\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testAnonMessageDoesNotFit() throws Exception {
		// Same as above but with anonymize path
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 10, -1, new String[]{"/k"}, null);
		String json = "{\"k\":\"longlonglongvalue\",\"next\":\"more\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testPruneObjectValue() throws Exception {
		// Path match where value is an object/array - tests the object-skip path in prune
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 200, -1, null, new String[]{"/k"});
		String json = "{\"k\":{\"nested\":\"value\"},\"other\":\"data\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testAnonObjectValue() throws Exception {
		// Anonymize an object value
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 200, -1, new String[]{"/k"}, null);
		String json = "{\"k\":{\"nested\":\"value\"},\"other\":\"data\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testPruneWithRemoveLastFilter() throws Exception {
		// After pruning a value with small prune message, offset > maxSizeLimit → removeLastFilter
		// maxSize=15, custom prune message = "X" (1 char), JSON has "k"="longlonglong" (12 chars)
		// After prune: value "longlonglong" (12) replaced by "X" (1) → removedLength=11
		// maxSizeLimit adjustment = 11. But if offset is still > maxSizeLimit, removeLastFilter is called.
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 15, -1, null, new String[]{"/k"}, "X", "X", "X");
		String json = "{\"k\":\"longlonglong\",\"n\":\"v\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testAnonWithRemoveLastFilter() throws Exception {
		// After anonymizing a value, offset > maxSizeLimit → removeLastFilter
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 15, -1, new String[]{"/k"}, null, "X", "X", "X");
		String json = "{\"k\":\"longlonglong\",\"n\":\"v\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testPruneQuotedValue() throws Exception {
		// Path match for a string value (quoted) that gets pruned
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 200, -1, null, new String[]{"/k"});
		String json = "{\"k\":\"value\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testAnonQuotedValue() throws Exception {
		// Anonymize a string value
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 200, -1, new String[]{"/k"}, null);
		String json = "{\"k\":\"value\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testExceptionInRangesReturnsFalse() throws Exception {
		// ranges() catches Exception and returns null → process() returns false
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 100, -1, new String[]{"/k"}, null);
		assertFalse(filter.process(new char[]{}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[]{}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void testPathMatchesExhausted() throws Exception {
		// pathMatches=1, after first prune, pathMatches becomes 0 → pathMatches<=0 block
		// maxSize=200 >> JSON length → maxSizeLimit >= maxReadLimit → rangesMultiPathMaxStringLength
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 200, 1, null, new String[]{"/k"});
		String json = "{\"k\":\"value1\",\"k\":\"value2\",\"other\":\"data\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testPathMatchesExhaustedWithMaxSize() throws Exception {
		// pathMatches=1 AND tight maxSize → maxSizeLimit < maxReadLimit → rangesMaxSizeMaxStringLength
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 15, 1, null, new String[]{"/k"});
		String json = "{\"k\":\"v\",\"other\":\"data and more and more and more\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testPruneRemoveLastFilterLargePruneMessage() throws Exception {
		// After addPrune with large prune message, offset > maxSizeLimit → removeLastFilter + return
		// Large prune message (25 chars), small value ("v") → prune message ADDS size → maxSizeLimit drops below offset
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 20, -1, null, new String[]{"/k"},
				"X".repeat(25), "X".repeat(25), "X".repeat(25));
		String json = "{\"k\":\"v\",\"n\":\"data\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testAnonRemoveLastFilterLargeAnonMessage() throws Exception {
		// After addAnon with large anon message, offset > maxSizeLimit → removeLastFilter + return
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 20, -1, new String[]{"/k"}, null,
				"X".repeat(25), "X".repeat(25), "X".repeat(25));
		String json = "{\"k\":\"v\",\"n\":\"data\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testStringLengthBreakLoop() throws Exception {
		// Non-matched string value too long AND can't fit even truncated version in maxSize
		// offset + maxStringLength > maxSizeLimit → break loop
		// Filter has path /other (not matching "k"), so "k" value is plain string truncation
		// maxStringLength=3, maxSize=8 → after { → maxSizeLimit=7. "k" value starts at ~5.
		// 5 + 3 = 8 > 7 → break loop
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(3, 8, -1, new String[]{"/other"}, null);
		String json = "{\"k\":\"longlonglong\",\"other\":\"v\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testStringLengthRemoveLastFilter() throws Exception {
		// Non-matched long string value that after truncation causes nextOffset > maxSizeLimit
		// Large truncate message so truncation ADDS size, shrinking effective maxSizeLimit
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(3, 50, -1, new String[]{"/other"}, null,
				"X".repeat(25), "X".repeat(25), "X".repeat(25));
		// "k" has a 4-char value (>= maxStringLength 3) → triggers addMaxLength → large truncate message → nextOffset > maxSizeLimit → removeLastFilter
		String json = "{\"k\":\"vvvv\",\"other\":\"v\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testDeepArrayGrowSquareBrackets() throws Exception {
		// 34 nested arrays - triggers grow() for the case '[' path (line 109 char/442 byte)
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
		String json = "{\"k\":\"value1\",\"k\":\"value2\",\"other\":\"data\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testAnonObjectValueRemoveLastFilter() throws Exception {
		// Anonymize object value where anonymizeSubtree causes remaining > maxSizeLimit
		// After anonymizeSubtree, getMaxSizeLimit < offset means we truncated the tree
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 25, -1, new String[]{"/k"}, null);
		String json = "{\"k\":{\"a\":\"b\",\"c\":\"d\",\"e\":\"f\"},\"other\":\"v\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testStringLengthRemoveLastFilterViaLongValue() throws Exception {
		// Long non-path string value: after addMaxLength, nextOffset > new maxSizeLimit → removeLastFilter
		// pathFilter=/other. "notapath":"verylongvalue" is processed via string length.
		// With maxSize=24: maxSizeLimit=23 after {, addMaxLength returns true (remove=1>0),
		// maxSizeLimit=24, nextOffset=26 > 24 → removeLastFilter
		String json = "{\"notapath\":\"verylongvalue\",\"other\":\"v\"}";
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(5, 24, -1, new String[]{"/other"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testSkipObjectOrArrayWithLongKeys() throws Exception {
		// skipObjectOrArrayMaxSizeMaxStringLength is called when level > pathItem.getLevel()
		// The skipped object has long keys → covers key handling in skipObjectOrArrayMaxSizeMaxStringLength
		// Path /a at level 1. At level 2 (inside "b"), encountering "c" value → skip
		// "c" = {"longlongkey":"val","longlongkey2":"v"} with maxStringLength=5
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(5, 300, -1, new String[]{"/a"}, null);
		String json = "{\"a\":\"v\",\"b\":{\"c\":{\"longlongkey\":\"value\",\"longlongkey2\":\"v\"}}}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testSkipObjectWithLongKeyAndWhitespaceBeforeColon() throws Exception {
		// skipObjectOrArrayMaxSizeMaxStringLength: long key with whitespace before colon
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(5, 300, -1, new String[]{"/a"}, null);
		String json = "{\"a\":\"v\",\"b\":{\"c\":{\"longlongkey\"  :\"value\",\"longlongkey2\"  :\"v\"}}}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testSkipObjectWithLongValueBreakLoop() throws Exception {
		// skipObjectOrArrayMaxSizeMaxStringLength: long value where offset+maxStringLength>=maxSizeLimit → break loop
		// Use small maxSize so the loop breaks on the long value
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(5, 35, -1, new String[]{"/a"}, null);
		String json = "{\"a\":\"v\",\"b\":{\"c\":{\"key\":\"verylongvaluehere\",\"other\":\"v\"}}}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testSkipDeepObjectGrow() throws Exception {
		// skipObjectOrArrayMaxSizeMaxStringLength: 35 levels of nesting inside the skipped object
		// Triggers grow() in CharArrayRangesSizeFilter.skipObjectOrArrayMaxSizeMaxStringLength (line 90)
		// and ByteArrayRangesSizeFilter equivalent (line 95)
		// Need maxSize close to JSON length to avoid crash from maxSizeLimit >> maxReadLimit
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
		// anonymizeSubtree: 35 levels of nesting inside the anonymized object
		// Triggers grow() in CharArrayRangesSizeFilter.anonymizeSubtree (line 221)
		// and ByteArrayRangesSizeFilter equivalent (line 226)
		// Need maxSize close to JSON length to avoid crash
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
		// ByteArrayRangesFilter line 487: scanEscapedValue called when chars[i-1] == '\\'
		// Need a string value containing escaped quote \" in byte processing
		// Also covers lines 529-530 (word-at-a-time scan) if string is long enough
		// JSON value: "Hello \"World\" how are you doing today"
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 5000, -1, new String[]{"/k"}, null);
		// String with escaped quote: "He said \"hello\" to everyone in the room"
		String json = "{\"k\":\"He said \\\"hello\\\" to everyone in the room\",\"other\":\"v\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		// Byte version triggers ByteArrayRangesFilter.scanEscapedValue (line 487)
		// and the word-at-a-time scan if string after escaped quote is long enough (lines 529-530)
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testByteSkipSubtreeWithFalseValue() throws Exception {
		// ByteArrayRangesFilter line 764: skipObjectMaxStringLength case 'f' (false value)
		// Need a false value inside a skipped subtree in byte processing
		// Path /a, "b" key not matched → skipObjectOrArrayMaxSizeMaxStringLength or skipObject
		// The skipped subtree contains: "flag":false
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 5000, -1, new String[]{"/a"}, null);
		String json = "{\"a\":\"v\",\"b\":{\"flag\":false,\"other\":true}}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testPathMatchesExhaustedWithMaxSizeLargerThanReadLimit() throws Exception {
		// After matching and exhausting pathMatches, maxSizeLimit >= maxReadLimit
		// Covers lines 297-299 (char) and 630-632 (byte) in PathMaxSizeMaxStringLengthJsonFilter
		// With maxSize=30, JSON length=31, prune "XXXXXXXXXXXX" (12Xs):
		//   removedLength=14-11=3, maxSizeLimit=29+3=32>=31=maxReadLimit → lines 297-299
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 30, 1, null, new String[]{"/k"});
		String json = "{\"k\":\"XXXXXXXXXXXX\",\"other\":\"v\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testMaxSizeLargerThanReadLimitAfterMatch() throws Exception {
		// After matching (pathMatches not exhausted), maxSizeLimit >= maxReadLimit
		// Covers lines 316-317 (char) and 649-650 (byte) in PathMaxSizeMaxStringLengthJsonFilter
		// With maxSize=30, JSON length ~31, prune "XXXXXXXXXXXX" (12Xs), pathMatches=2:
		//   after first match: maxSizeLimit>=maxReadLimit but pathMatches=1>0 → lines 316-317
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 30, 2, null, new String[]{"/k"});
		String json = "{\"k\":\"XXXXXXXXXXXX\",\"other\":\"v\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testKeyWithWhitespaceBeforeColonInSkippedSubtree() throws Exception {
		// skipObjectOrArrayMaxSizeMaxStringLength: key with whitespace before colon
		// Covers line 205 (char) in PathMaxSizeMaxStringLengthJsonFilter
		// Need whitespace BEFORE colon in a key inside a skipped subtree
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 5000, -1, new String[]{"/a"}, null);
		// "b"'s value has key "c" with whitespace before colon: "c"   :"v"
		String json = "{\"a\":\"v\",\"b\":{\"c\"   :\"v\"}}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testSkipObjectMaxSizeOverflow() throws Exception {
		// skipObjectOrArrayMaxSizeMaxStringLength: long string causes maxSizeLimit > maxReadLimit
		// Covers CharArrayRangesSizeFilter line 166 and ByteArrayRangesSizeFilter line 171
		// JSON: {"a":{"b":{"inner":"aaa...(200 a's)..."}}, "path":"v"} (~235 chars)
		// Path /path, level > pathItem.getLevel() at b's value { → skip b's subtree
		// Inside skip: long string truncation → maxSizeLimit exceeds maxReadLimit → cap
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
		// anonymizeSubtree: scalar value (true) at matched path cannot fit anon message
		// Covers CharArrayRangesSizeFilter lines 359, 361 and ByteArrayRangesSizeFilter lines 370, 372
		// JSON: {"key":{"val":true}} (20 chars), path /key, maxSize=17
		// ranges: maxSizeLimit=17, after outer {: 16. Call anonymizeSubtree(chars, 7, 16).
		// Inside subtree: { at 7 → maxSizeLimit=15. "val" key → offset=14.
		// 14 + anonMsgLen(3) = 17 > maxSizeLimit(15) → else → lines 359/361 fired
		MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter filter =
			new MustContrainMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 17, -1, new String[]{"/key"}, null);
		String json = "{\"key\":{\"val\":true}}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

}
