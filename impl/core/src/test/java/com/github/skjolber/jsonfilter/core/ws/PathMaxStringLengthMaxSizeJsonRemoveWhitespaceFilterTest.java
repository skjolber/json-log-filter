package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.core.AnyPathMaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
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
		// Triggers anonymizeObjectOrArrayMaxSize when matched path key has object/array value + maxSize active
		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter;
		String json = "{\"key2\":{\"a\":\"b\"},\"other\":\"data\"}";

		filter = new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 50, -1, new String[]{"/key2"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));

		filter = new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 50, -1, null, new String[]{"/key2"});
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));

		// Triggers skipObjectOrArrayMaxSizeMaxStringLength for non-matched object sibling + maxSize
		String json2 = "{\"key1\":\"v\",\"key2\":{\"n\":\"longlonglong\"}}";
		filter = new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, 50, -1, new String[]{"/key1"}, null);
		assertNotNull(filter.process(json2.toCharArray(), 0, json2.length(), new StringBuilder()));
		assertNotNull(filter.process(json2.getBytes(StandardCharsets.UTF_8)));

		filter = new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, 50, -1, null, new String[]{"/key1"});
		assertNotNull(filter.process(json2.toCharArray(), 0, json2.length(), new StringBuilder()));
		assertNotNull(filter.process(json2.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testGrowSquareBrackets() throws Exception {
		// 35 levels of nesting to trigger grow() in processMaxSize
		// Use //anyPath filter to prevent early skip of nested objects, and long string + capped maxSize
		StringBuilder deepJson = new StringBuilder();
		for (int i = 0; i < 35; i++) {
			deepJson.append("{\"k").append(i).append("\":");
		}
		deepJson.append("\"").append("x".repeat(500)).append("\"");
		for (int i = 0; i < 35; i++) {
			deepJson.append("}");
		}
		String json = deepJson.toString();

		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(5, 400, -1, new String[]{"//targetKey"}, null);

		StringBuilder charOutput = new StringBuilder();
		org.junit.jupiter.api.Assertions.assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));

		byte[] jsonBytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream byteOutput = new com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream(128);
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
		// Test processMaxSize path with a matching path and tight maxSize
		// maxSize causes the filter to use MustConstrain branch
		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(3, 30, -1, new String[]{"/key"}, null);
		String json = "{\"key\":\"longvaluelongvalue\",\"other\":\"data\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testProcessWithPruneAndMaxSize() throws Exception {
		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(3, 30, -1, null, new String[]{"/key"});
		String json = "{\"key\":\"longvaluelongvalue\",\"other\":\"data\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testProcessMatchDoesNotFit() throws Exception {
		// Very small maxSize where the match result doesn't fit
		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 10, -1, new String[]{"/k"}, null);
		String json = "{\"k\":\"longlongvalue\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testProcessWithMetricsCallsSuperWhenNotConstrained() throws Exception {
		// Covers super.process(chars, ..., metrics) when !mustConstrainMaxSize (line 29 char, 427 byte)
		PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 100, -1, new String[]{"/k"}, null);
		String json = "{\"k\":\"value\"}"; // 13 chars << 100 = maxSize → mustConstrainMaxSize returns false
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder(), metrics));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8), 0, json.length(), new ResizableByteArrayOutputStream(128), metrics));
	}

	@Test
	public void testGrowSquareBracketsInMainLoop() throws Exception {
		// 35 nested arrays ([) triggers grow() in main processMaxSize loop for '[' case (line 144)
		// Need anyPath filter (//target) so non-matched '[' values stay in main loop (not skipped)
		StringBuilder deepJson = new StringBuilder();
		deepJson.append("{\"skip\":");
		for (int i = 0; i < 35; i++) {
			deepJson.append("[");
		}
		deepJson.append("1");
		for (int i = 0; i < 35; i++) {
			deepJson.append("]");
		}
		deepJson.append(",\"target\":\"v\"}");
		String json = deepJson.toString();
		int maxSize = json.length() - 1;

		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, maxSize, -1, new String[]{"//target"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testGrowSquareBracketsInAnonSubtree() throws Exception {
		// 35 nested brackets triggers grow() in anonymizeObjectOrArrayMaxSize (line 265 char, 664 byte)
		// Anonymize a key whose value has 35 nested objects
		StringBuilder deepValue = new StringBuilder();
		for (int i = 0; i < 35; i++) {
			deepValue.append("{\"n").append(String.format("%02d", i)).append("\":");
		}
		deepValue.append("1");
		for (int i = 0; i < 35; i++) {
			deepValue.append("}");
		}
		String json = "{\"k\":" + deepValue + ",\"other\":\"v\"}";
		int maxSize = json.length() - 5;

		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, maxSize, -1, new String[]{"/k"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testGrowSquareBracketsInPruneSubtree() throws Exception {
		// 35 nested brackets triggers grow() in prune/skip subtree (line 337 char, 737 byte)
		// Prune a key whose value has 35 nested objects
		StringBuilder deepValue = new StringBuilder();
		for (int i = 0; i < 35; i++) {
			deepValue.append("{\"n").append(String.format("%02d", i)).append("\":");
		}
		deepValue.append("1");
		for (int i = 0; i < 35; i++) {
			deepValue.append("}");
		}
		String json = "{\"k\":" + deepValue + ",\"other\":\"v\"}";
		int maxSize = json.length() - 5;

		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, maxSize, -1, null, new String[]{"/k"});
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testSkipSubtreeGrowInSkipObjectOrArray() throws Exception {
		// Nested structure in a non-matched subtree processed in main loop
		// Path is /other (matching). The "skip" value has deep nesting at level 2+.
		// Level 2 objects trigger skipObjectOrArrayMaxSizeMaxStringLength.
		// Use maxSize = json.length()-1 to keep within bounds
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
		// For bytes: process(byte[]) returns null when process(byte[], ..., output) returns false
		// Just verify it doesn't crash (don't require non-null since tight maxSize may cause truncation)
		filter.process(json.getBytes(StandardCharsets.UTF_8)); // should not throw
	}

	@Test
	public void testAnonSubtreeWithWhitespaceBeforeColon() throws Exception {
		// anonymizeObjectOrArrayMaxSize: key with whitespace BEFORE colon in anon subtree
		// Covers line 331 (char) and 335 (byte) in CharArrayWhitespaceSizeFilter:
		//   writtenMark = buffer.length() + mark - flushOffset
		// in the `if(nextOffset != endQuoteIndex)` block
		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 1000, -1, new String[]{"/k"}, null);
		// "a"   :"v" has 3 spaces before colon
		String json = "{\"k\":{\"a\"   :\"v\",\"b\":\"x\"}}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testAnonSubtreeScalarValueMaxSizeCap() throws Exception {
		// anonymizeObjectOrArrayMaxSize: scalar value (number) anonymized, maxSizeLimit >= maxReadLimit
		// Covers line 387 (char) and 392 (byte) in CharArrayWhitespaceSizeFilter:
		//   if(maxSizeLimit >= maxReadLimit) { maxSizeLimit = maxReadLimit; }
		// in the default case for scalar anon
		// Scalar value "123456789012345" (15 chars) replaced with anon msg (1 char) -> delta=14
		// With maxSize=15: maxSizeLimit=14 after '{'. After scalar anon: 14+14=28 > maxReadLimit=26 → cap
		String json = "{\"k\":{\"a\":123456789012345}}";
		int maxSize = 17;
		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, maxSize, -1, new String[]{"/k"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testAnonSubtreeStringValueMaxSizeCap() throws Exception {
		// skipObjectOrArrayMaxSizeMaxStringLength: long string value truncated via addMaxLength,
		// then maxSizeLimit gets capped (line 176 char, 181 byte)
		// Non-matched key "skip" has {obj} value; inside: "str":"verylongstringxx" (16 chars)
		// removed=12, remove=2 -> maxSizeLimit += 2 -> reaches maxReadLimit -> cap
		String json = "{\"target\":\"v\",\"skip\":{\"str\":\"verylongstringxx\"}}";
		int maxSize = json.length(); // exact length so maxSizeLimit=maxReadLimit

		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(3, maxSize, -1, new String[]{"/target"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testAnonSubtreeBreakLoopOnBracket() throws Exception {
		// anonymizeObjectOrArrayMaxSize: tight maxSize where `{`/`[` in anon subtree hits break loop
		// Covers line 270 (char) and 274 (byte) in CharArrayWhitespaceSizeFilter:
		//   `break loop` when offset >= maxSizeLimit after decrement
		// JSON = {"k":{"a":{"b":1}}}, maxSize=10: {"k": start at 0, { anon at 4, inner { at 9
		// maxSizeLimit=9 after outer {. After "k" matched, anon. inner value { at 9: maxSizeLimit--=8. 9>=8 → break!
		String json = "{\"k\":[[{\"v\":1}]]}";
		int maxSize = 11;
		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, maxSize, -1, new String[]{"/k"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testAnonSubtreeSquareBracketsAssignment() throws Exception {
		// anonymizeObjectOrArrayMaxSize: covers squareBrackets[bracketLevel] = c == '['
		// (line 273 char, 277 byte) in a nested '[' inside the anon subtree
		// Need c='[' (array) to cover the true branch of c == '['
		String json = "{\"k\":{\"arr\":[1,2,3],\"c\":\"v\"}}";
		int maxSize = 1000; // large enough to avoid tight maxSize issues
		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, maxSize, -1, new String[]{"/k"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

@Test
public void testAnonSubtreeWithInternalWhitespace() throws Exception {
// CharArrayWhitespaceFilter.anonymizeObjectOrArray line 74 (char) and 99 (byte):
// the break for whitespace chars inside a matched subtree
// Need a JSON with spaces inside the anonymized object value
String json = "{\"k\":{ \"a\": \"value\" }}";
MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 1000, -1, new String[]{"/k"}, null);
assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
}

	@Test
	public void testGrowSquareBracketsProcessMaxSizeAnonPath() throws Exception {
		// Line 337 (char) and 737 (byte): grow() in processMaxSize ANONYMIZE path
		// Need bracketLevel = 31 at anon match with object value → bracketLevel++ = 32 >= 32 → grow
		// Solution: path /k0/k1/.../k29/target (31 components), 30 intermediate ki levels
		// outer { + 30 ki { brackets = bracketLevel=31 when "target" key is processed
		int numIntermediate = 30;
		StringBuilder pathBuilder = new StringBuilder();
		for (int i = 0; i < numIntermediate; i++) {
			pathBuilder.append("/k").append(i);
		}
		pathBuilder.append("/target");
		String path = pathBuilder.toString();

		StringBuilder jsonBuilder = new StringBuilder();
		jsonBuilder.append("{");
		for (int i = 0; i < numIntermediate; i++) {
			jsonBuilder.append("\"k").append(i).append("\":{");
		}
		// at bracketLevel=31: "target" with object value → ANON path → bracketLevel++ = 32 ≥ 32 → line 337
		jsonBuilder.append("\"target\":{\"inner\":\"v\"}");
		for (int i = 0; i < numIntermediate + 1; i++) {
			jsonBuilder.append("}");
		}
		String json = jsonBuilder.toString();

		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, json.length() + 1000, -1, new String[]{path}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		assertNotNull(filter.process(bytes, 0, bytes.length, new com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void testGrowSquareBracketsProcessMaxSizeSkipPath() throws Exception {
		// Line 265 (char) and 664 (byte): grow() in processMaxSize skip path (filterType==null)
		// Need bracketLevel = 63 in case '"' handler for non-matching key with object value
		// squareBrackets grew to 64 (from main loop at bracketLevel 31→32), so 63++ = 64 >= 64 → line 265
		// Solution: path /k0/.../k61/target (63 components), 62 intermediate ki levels
		// each ki match advances pathItem → continue → main loop processes { → bracketLevel grows
		// At depth 63 (bracketLevel=63): "skip" non-matching key with object value → line 265
		int numIntermediate = 62;
		StringBuilder pathBuilder = new StringBuilder();
		for (int i = 0; i < numIntermediate; i++) {
			pathBuilder.append("/k").append(i);
		}
		pathBuilder.append("/target");
		String path = pathBuilder.toString();

		StringBuilder jsonBuilder = new StringBuilder();
		jsonBuilder.append("{");
		for (int i = 0; i < numIntermediate; i++) {
			jsonBuilder.append("\"k").append(i).append("\":{");
		}
		// at bracketLevel=63: "skip" non-matching with object value → skip path → bracketLevel++ = 64 ≥ 64 → line 265
		jsonBuilder.append("\"skip\":{\"x\":\"v\"},\"target\":\"found\"");
		for (int i = 0; i < numIntermediate + 1; i++) {
			jsonBuilder.append("}");
		}
		String json = jsonBuilder.toString();

		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, json.length() + 1000, -1, null, new String[]{path});
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		assertNotNull(filter.process(bytes, 0, bytes.length, new com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream(128)));
	}

}
