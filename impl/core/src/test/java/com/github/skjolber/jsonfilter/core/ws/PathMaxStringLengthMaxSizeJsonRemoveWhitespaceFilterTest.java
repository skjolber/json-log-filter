package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.core.AnyPathMaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.Generator;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair.MaxSizeJsonFilterFunction;

public class PathMaxStringLengthMaxSizeJsonRemoveWhitespaceFilterTest extends DefaultJsonFilterTest {

	
	private static class MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter extends PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter {

		public MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(int maxStringLength, int maxSize,
				int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage,
				String truncateMessage) {
			super(maxStringLength, maxSize, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
		}

		public MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(int maxStringLength, int maxSize,
				int maxPathMatches, String[] anonymizes, String[] prunes) {
			super(maxStringLength, maxSize, maxPathMatches, anonymizes, prunes);
		}

		@Override
		protected boolean mustConstrainMaxSize(int length) {
			return true;
		}
	};
	
	public PathMaxStringLengthMaxSizeJsonRemoveWhitespaceFilterTest() throws Exception {
		super();
	}

	@Test
	@ResourceLock(value = "jackson")
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(128, size, -1, new String[] {"/CVE_Items/cve/CVE_data_meta"}, null));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(128, size, -1, null, new String[] {"/CVE_Items/cve/CVE_data_meta"}));
		
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(128, size, -1,new String[] {"/CVE_Items/impact/baseMetricV2/severity"}, null));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(128, size, -1,null, new String[] {"/CVE_Items/impact/baseMetricV2/severity"}));

		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(128, size, -1,new String[] {"/CVE_Items/impact/baseMetricV2/impactScore"}, null));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(128, size, -1,null, new String[] {"/CVE_Items/impact/baseMetricV2/impactScore"}));
		
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(128, size, -1,new String[] {"/CVE_Items/impact/baseMetricV2/obtainAllPrivilege"}, null));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(128, size, -1,null, new String[] {"/CVE_Items/impact/baseMetricV2/obtainAllPrivilege"}));
	}
	
	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(128, size, -1, new String[] {"/CVE_Items/cve/CVE_data_meta"}, null));
	}
	
	@Test
	public void testDeepStructure2() throws IOException {
		validateDeepStructure( (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(128, size, -1, new String[] {DEEP_PATH}, null));
	}
	
	@Test
	public void passthrough_success() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, -1, null, null);
		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, null, null)).hasPassthrough();
		
		maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, -1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasPassthrough();
	}
	
	@Test
	public void anonymize() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, -1, new String[]{DEFAULT_PATH}, null);
		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		
		maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, -1, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		
		maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, -1, new String[]{DEEP_PATH1, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, new String[]{DEEP_PATH1, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEEP_PATH1).hasAnonymizeMetrics();
	}
	
	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, 1, new String[]{"/key1"}, null);
		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, 1, new String[]{"/key1"}, null)).hasAnonymized("/key1");
		

		maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, 1, new String[]{DEFAULT_PATH}, null);
		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, 1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		
		maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, 2, new String[]{DEFAULT_PATH}, null);
		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, 2, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
	}

	@Test
	public void anonymizeWildcard() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, -1, new String[]{DEFAULT_WILDCARD_PATH}, null);
		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, new String[]{DEFAULT_WILDCARD_PATH}, null)).hasAnonymized(DEFAULT_WILDCARD_PATH).hasAnonymizeMetrics();
	}
	
	@Test
	public void anonymizeAny() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, -1, new String[]{DEFAULT_ANY_PATH}, null);
		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
	}

	@Test
	public void prune() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, -1, null, new String[]{DEFAULT_PATH});
		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		
		maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, -1, null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH});
		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		
		maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, -1, null, new String[]{DEEP_PATH3, PASSTHROUGH_XPATH});
		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, null, new String[]{DEEP_PATH3, PASSTHROUGH_XPATH})).hasPruned(DEEP_PATH3).hasPruneMetrics();
	}
	
	@Test
	public void pruneMaxPathMatches() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, 1, null, new String[]{"/key3"});
		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, 1, null, new String[]{"/key3"})).hasPruned("/key3").hasPruneMetrics();
		
		maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, 1, null, new String[]{DEFAULT_PATH});
		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, 1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		
		maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, 2, null, new String[]{DEFAULT_PATH});
		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, 2, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
	}

	@Test
	public void pruneWildcard() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, -1, null, new String[]{DEFAULT_WILDCARD_PATH});
		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, null, new String[]{DEFAULT_WILDCARD_PATH})).hasPruned(DEFAULT_WILDCARD_PATH).hasPruneMetrics();
	}
	
	@Test
	public void pruneAny() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, -1, null, new String[]{DEFAULT_ANY_PATH});
		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();
	}	

	@Test
	public void maxStringLength() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, -1, null, null);
		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, null, null)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH).hasMaxStringLengthMetrics();
	}
	
	@Test
	public void maxStringLengthAnonymizePrune() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, 2, new String[]{"/key1"}, new String[]{"/key3"});

		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, 2, new String[]{"/key1"}, new String[]{"/key3"}))
			.hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH)
			.hasMaxPathMatches(2)
			.hasPruned("/key3").hasPruneMetrics()
			.hasAnonymized("/key1").hasAnonymizeMetrics();
	}

	@Test
	public void exception_returns_false() throws Exception {
		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter = new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, -1, -1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void testAnonymizeObjectValueWithMaxSize() throws Exception {
		// Anonymizing an object value at the matched path when maxSize is active must produce valid output.
		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter;
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/maxSize/objectKey2NestedOther.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);

		filter = new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 50, -1, new String[]{"/key2"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));

		filter = new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 50, -1, null, new String[]{"/key2"});
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));

		// Skipping a non-matched sibling that contains a nested object, when both maxSize and string-length limits are active, must produce valid output.
		byte[] json2Bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/maxSize/objectKey1ShortKey2NestedLong.json"));
		String json2 = new String(json2Bytes, StandardCharsets.UTF_8);
		filter = new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, 50, -1, new String[]{"/key1"}, null);
		assertNotNull(filter.process(json2.toCharArray(), 0, json2.length(), new StringBuilder()));
		assertNotNull(filter.process(json2Bytes));

		filter = new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, 50, -1, null, new String[]{"/key1"});
		assertNotNull(filter.process(json2.toCharArray(), 0, json2.length(), new StringBuilder()));
		assertNotNull(filter.process(json2Bytes));
	}

	@Test
	public void testGrowSquareBrackets() throws Exception {
		// 35 levels of nesting forces the filter's bracket-tracking array to grow beyond its initial capacity.
		// An any-path filter ensures all nested objects are traversed rather than skipped.
		byte[] jsonBytes = Generator.generateDeepObjectStructure(35, "x".repeat(500), false);
		String json = new String(jsonBytes, StandardCharsets.UTF_8);

		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(5, 400, -1, new String[]{"//targetKey"}, null);

		StringBuilder charOutput = new StringBuilder();
		org.junit.jupiter.api.Assertions.assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		org.junit.jupiter.api.Assertions.assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput));
	}

	@Test
	public void testExceptionWithMetricsReturnsFalse() throws Exception {
		// process(char/byte, ..., JsonFilterMetrics) returns false on exception
		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(3, 100, -1, new String[]{"/k"}, null);
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		assertFalse(filter.process(new char[]{}, 1, 1, new StringBuilder(), metrics));
		assertFalse(filter.process(new byte[]{}, 1, 1, new ResizableByteArrayOutputStream(128), metrics));
	}

	@Test
	public void testProcessWithMatchAndMaxSize() throws Exception {
		// Filtering a matched path with a tight maxSize constraint must produce valid output.
		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(3, 30, -1, new String[]{"/key"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/maxSize/objectKeyLongvaluelongvalueOther.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testProcessWithPruneAndMaxSize() throws Exception {
		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(3, 30, -1, null, new String[]{"/key"});
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/maxSize/objectKeyLongvaluelongvalueOther.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testProcessMatchDoesNotFit() throws Exception {
		// Filtering a matched value when the output would exceed maxSize must produce valid output.
		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 10, -1, new String[]{"/k"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/maxSize/objectKLonglongvalue.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testProcessWithMetricsCallsSuperWhenNotConstrained() throws Exception {
		// When the document is much smaller than maxSize, the filter delegates to the base path-filter without size constraints.
		PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 100, -1, new String[]{"/k"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/shortKey/objectKValue.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder(), metrics));
		assertNotNull(filter.process(jsonBytes, 0, json.length(), new ResizableByteArrayOutputStream(128), metrics));
	}

	@Test
	public void testGrowSquareBracketsInMainLoop() throws Exception {
		// 35 nested arrays force the bracket-tracking array to grow. An any-path filter ensures all arrays are traversed rather than skipped.
		byte[] jsonBytes = Generator.generateObjectWithDeepArrayValue(35, "skip", "target", "v", false);
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		int maxSize = json.length() - 1;

		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, maxSize, -1, new String[]{"//target"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testGrowSquareBracketsInAnonSubtree() throws Exception {
		// The anonymized value contains 35 levels of nesting, forcing the filter's bracket-tracking to grow during subtree anonymization.
		byte[] jsonBytes = Generator.generateObjectWithDeepObjectValue(35, "k", "other", "v", false);
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		int maxSize = json.length() - 5;

		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, maxSize, -1, new String[]{"/k"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testGrowSquareBracketsInPruneSubtree() throws Exception {
		// The pruned value contains 35 levels of nesting, forcing the filter's bracket-tracking to grow during subtree pruning.
		byte[] jsonBytes = Generator.generateObjectWithDeepObjectValue(35, "k", "other", "v", false);
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		int maxSize = json.length() - 5;

		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, maxSize, -1, null, new String[]{"/k"});
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testSkipSubtreeGrowInSkipObjectOrArray() throws Exception {
		// A non-matched sibling with a moderately deep nested value triggers the skip-object path for inner nesting.
		StringBuilder deepValue = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			deepValue.append("{\"n").append(i).append("\":");
		}
		deepValue.append("1");
		for (int i = 0; i < 10; i++) {
			deepValue.append("}");
		}
		String json = "{\"other\":\"v\",\"skip\":" + deepValue + "}";
		int maxSize = json.length() - 1;

		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, maxSize, -1, new String[]{"/other"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		filter.process(json.getBytes(StandardCharsets.UTF_8)); // should not throw
	}

	@Test
	public void testAnonSubtreeWithWhitespaceBeforeColon() throws Exception {
		// Verifies that keys with whitespace before the colon inside an anonymized subtree are handled correctly.
		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 1000, -1, new String[]{"/k"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/maxSize/objectKNestedWhitespaceSibling.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testAnonSubtreeScalarValueMaxSizeCap() throws Exception {
		// Verifies that anonymizing a scalar value inside an object subtree where the size limit equals the document length is handled correctly.
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/maxSize/objectKNestedLongNumber.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		int maxSize = 17;
		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter = new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, maxSize, -1, new String[]{"/k"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testAnonSubtreeStringValueMaxSizeCap() throws Exception {
		// Verifies that truncating a long string inside a skipped (non-matched) object subtree, where the size limit equals the document length, is handled correctly.
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/maxSize/objectTargetValueSkipNested.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		int maxSize = json.length();

		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(3, maxSize, -1, new String[]{"/target"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testAnonSubtreeBreakLoopOnBracket() throws Exception {
		// Verifies that a nested bracket inside an anonymized subtree that pushes the position past the size limit causes the filter to stop and truncate cleanly.
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/maxSize/objectKNestedArrayLevels.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		int maxSize = 11;
		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter = new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, maxSize, -1, new String[]{"/k"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testAnonSubtreeSquareBracketsAssignment() throws Exception {
		// Verifies that an array nested inside an anonymized object subtree is handled correctly.
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/maxSize/objectKNestedObjectWithArray.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		int maxSize = 1000;
		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter = new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, maxSize, -1, new String[]{"/k"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes));
	}

@Test
public void testAnonSubtreeWithInternalWhitespace() throws Exception {
	// Verifies that whitespace inside an anonymized object value is handled correctly.
	byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/maxSize/objectKNestedWhitespaceValue.json"));
	String json = new String(jsonBytes, StandardCharsets.UTF_8);
	MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter = new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 1000, -1, new String[]{"/k"}, null);
	assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
	assertNotNull(filter.process(jsonBytes));
}

	@Test
	public void testGrowSquareBracketsProcessMaxSizeAnonPath() throws Exception {
		// The target path is nested 31 levels deep so that when the matched key's object value is entered, the bracket-tracking array must grow. Uses an anonymize action on the matched path.
		int numIntermediate = 30;
		StringBuilder pathBuilder = new StringBuilder();
		for (int i = 0; i < numIntermediate; i++) {
			pathBuilder.append("/k").append(i);
		}
		pathBuilder.append("/target");
		String path = pathBuilder.toString();

		byte[] jsonBytes = Generator.generateDeepPathWithObjectLeaf(numIntermediate, "target", false);
		String json = new String(jsonBytes, StandardCharsets.UTF_8);

		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter = new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, json.length() + 1000, -1, new String[]{path}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes, 0, jsonBytes.length, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void testGrowSquareBracketsProcessMaxSizeSkipPath() throws Exception {
		// The target path is nested 63 levels deep so that when a non-matching sibling key's object value is entered, the already-grown bracket-tracking array must grow again. Uses a prune action on the matched path.
		int numIntermediate = 62;
		StringBuilder pathBuilder = new StringBuilder();
		for (int i = 0; i < numIntermediate; i++) {
			pathBuilder.append("/k").append(i);
		}
		pathBuilder.append("/target");
		String path = pathBuilder.toString();

		byte[] jsonBytes = Generator.generateDeepPathWithSiblings(numIntermediate, "skip", "target", "found", false);
		String json = new String(jsonBytes, StandardCharsets.UTF_8);

		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter = new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, json.length() + 1000, -1, null, new String[]{path});
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonBytes, 0, jsonBytes.length, new ResizableByteArrayOutputStream(128)));
	}

}
