package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
	public void maxStringLengthTopLevelStringLongerThanMaxSize() throws Exception {
		// a string cannot be cut, so it is only truncated by max string length, even if longer than max size
		String string = " \"hello world, a long string\"\n";
		String truncated = "\"hello... + 21\"";
		PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter = new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, 10, -1, new String[]{DEFAULT_PATH}, new String[]{PASSTHROUGH_XPATH});

		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		assertEquals(truncated, filter.process(string, metrics));
		assertArrayEquals(truncated.getBytes(StandardCharsets.UTF_8), filter.process(string.getBytes(StandardCharsets.UTF_8), metrics));
		assertEquals(2, metrics.getMaxStringLength());

		// a colon after the slice must not make the string look like a key
		String input = "x" + string + ":y";

		StringBuilder builder = new StringBuilder();
		assertTrue(filter.process(input.toCharArray(), 1, string.length(), builder));
		assertEquals(truncated, builder.toString());

		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(input.getBytes(StandardCharsets.UTF_8), 1, string.length(), output));
		assertEquals(truncated, output.toString(StandardCharsets.UTF_8));
	}

	@Test
	public void testNonStructuralIsKept() {
		// there is no smaller valid document
		JsonFilter[] filters = {
			new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 2, -1, new String[]{"/a"}, null),
			new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(5, 2, -1, new String[]{"/a"}, null),
			new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 2, -1, new String[]{"//a"}, null)
		};
		for(JsonFilter filter : filters) {
			for(String json : new String[] {"\"abc\"", "-1.5e3", "123456", "true", "false", "null"}) {
				assertEquals(json, filter.process(json));
				assertEquals(json, new String(filter.process(json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
			}
		}
	}

	@Test
	public void maxStringLengthUnmatchedValues() throws Exception {
		// string values of keys which do not match the path are truncated too, also when max size is active
		assertMaxStringLengthMaxSize("{\"a\":\"xxxxx... + 25\",\"b\":{\"c\":\"yyyyy... + 25\"},\"d\":1}", "/json/text/maxSize/objectALongBNestedCLongDNumber.json", 60);
		assertMaxStringLengthMaxSize("{\"k\":\"zzzzz... + 35\"}", "/json/text/maxSize/objectKLongString.json", 30);
		assertMaxStringLengthMaxSize("{\"k\":\"zzzzz... + 35\",\"z\":1}", "/json/text/maxSize/objectKLongStringZNumber.json", 30);
		assertMaxStringLengthMaxSize("{\"k\":\"zzzzz... + 35\",\"z\":1}", "/json/text/maxSize/objectKLongStringZNumberWhitespace.json", 30);
	}

	private void assertMaxStringLengthMaxSize(String expected, String resource, int maxSize) throws IOException {
		PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter = new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(5, maxSize, -1, new String[]{"/x"}, null);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream(resource));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);

		org.junit.jupiter.api.Assertions.assertEquals(expected, filter.process(json));
		org.junit.jupiter.api.Assertions.assertEquals(expected, new String(filter.process(jsonBytes), StandardCharsets.UTF_8));
	}

	@Test
	public void exception_returns_false() throws Exception {
		MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter = new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, -1, -1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void exception_leaves_output_untouched() throws Exception {
		// max size below the input size, but large enough to reach the end of the input
		PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter = new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, UNTERMINATED_LAST_STRING.length() - 1, -1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertExceptionLeavesOutputUntouched(filter, UNTERMINATED_LAST_STRING);
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
	public void testValueFollowedByWhitespaceMustFit() throws Exception {
		// the whitespace after a value is not written, but does not make room for the value itself
		PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter = new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 10, -1, new String[]{"/zz"}, null);

		String json = "[\"" + "v".repeat(200) + "\"" + " ".repeat(300) + "]";
		org.junit.jupiter.api.Assertions.assertEquals("[]", filter.process(json));
		org.junit.jupiter.api.Assertions.assertEquals("[]", new String(filter.process(json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));

		json = "[\"vvvvvv\"" + " ".repeat(300) + "]";
		org.junit.jupiter.api.Assertions.assertEquals("[\"vvvvvv\"]", filter.process(json));
		org.junit.jupiter.api.Assertions.assertEquals("[\"vvvvvv\"]", new String(filter.process(json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
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
	public void testMaxSizeWithinAnonymizedSubtree() throws Exception {
		// max size reached within an anonymized subtree: the string value which does not fit is left out, not cut
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, -1, new String[]{"/secret"}, null);
		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, new String[]{"/secret"}, null)).hasAnonymized("/secret");
	}

	@Test
	public void testMaxPathMatchesUsedUpWithinAnonymizedSubtree() throws Exception {
		// max path matches used up by a subtree which was cut by max size
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, 1, new String[]{"//secret"}, null);
		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, 1, new String[]{"//secret"}, null)).hasAnonymized("//secret");
	}

	@Test
	public void testMessageLongerThanValueWithWhitespaceAroundRoot() throws Exception {
		// whitespace around the root value makes the input longer than max size, while the prune and
		// anonymize messages are longer than the values they replace
		// inline: files in /json must end with the root value, see SkipObjectJsonFilterTest
		String trailing = "{\"key\":10}" + " ".repeat(40);
		String leading = " ".repeat(10) + "{\"key\":10}";
		String sibling = "{\"key\":1,\"b\":2}" + " ".repeat(40);
		String[] key = new String[]{"//key"};

		assertRemoveWhitespaceMaxSize("{}", trailing, 10, key, null);
		assertRemoveWhitespaceMaxSize("{\"key\":\"*\"}", trailing, 11, key, null);
		assertRemoveWhitespaceMaxSize("{\"key\":\"*\"}", trailing, 49, key, null);
		assertRemoveWhitespaceMaxSize("{\"key\":\"*\"}", leading, 11, new String[]{"/key"}, null);
		assertRemoveWhitespaceMaxSize("{\"key\":\"*\"}", sibling, 16, key, null);
		assertRemoveWhitespaceMaxSize("{\"key\":\"*\",\"b\":2}", sibling, 17, key, null);

		assertRemoveWhitespaceMaxSize("{}", trailing, 18, null, key);
		assertRemoveWhitespaceMaxSize("{\"key\":\"[removed]\"}", trailing, 19, null, key);
		assertRemoveWhitespaceMaxSize("{\"key\":\"[removed]\"}", leading, 19, null, key);
		assertRemoveWhitespaceMaxSize("{\"key\":\"[removed]\"}", sibling, 24, null, key);
		assertRemoveWhitespaceMaxSize("{\"key\":\"[removed]\",\"b\":2}", sibling, 25, null, key);
	}

	private void assertRemoveWhitespaceMaxSize(String expected, String json, int maxSize, String[] anonymizes, String[] prunes) {
		PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter = new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, maxSize, -1, anonymizes, prunes);
		assertEquals(expected, filter.process(json));
		assertEquals(expected, new String(filter.process(json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
	}

	@Test
	public void testWildcardPathAfterMatchingSibling() throws Exception {
		// the sibling "b" also matches the wildcard
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size, -1, new String[]{"/*/secret"}, null);
		assertThat(maxSize, new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, new String[]{"/*/secret"}, null)).hasAnonymized("/*/secret");
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
	public void testPruneMessagesLongerThanValues() throws Exception {
		// The document fits within max size, but the output does not: each prune message is longer than
		// the value it replaces (46 chars in, 96 chars out without max size). The output is rolled back
		// and the document is filtered again with max size.
		String json = "[{\"id\":0},{\"id\":0},{\"id\":0},{\"id\":0},{\"id\":0}]";
		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		String expected = "[{\"id\":\"[removed]\"},{\"id\":\"[removed]\"},{}]";

		PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, json.length(), -1, null, new String[]{"//id"});

		// output written before the document is kept
		StringBuilder charOutput = new StringBuilder("[1,");
		org.junit.jupiter.api.Assertions.assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		org.junit.jupiter.api.Assertions.assertEquals("[1," + expected, charOutput.toString());

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		byteOutput.write("[1,".getBytes(StandardCharsets.UTF_8));
		org.junit.jupiter.api.Assertions.assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput));
		org.junit.jupiter.api.Assertions.assertEquals("[1," + expected, byteOutput.toString(StandardCharsets.UTF_8));
	}

	@Test
	public void testMetricsForPruneMessagesLongerThanValues() throws Exception {
		// The document is first filtered without max size, and again with max size if that output is too large.
		// The metrics then include both passes.
		String json = "[{\"id\":0},{\"id\":0},{\"id\":0},{\"id\":0},{\"id\":0}]";
		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

		// output too large, filtered again with max size: 5 pruned in the first pass, and 2 in the second
		PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, json.length(), -1, null, new String[]{"//id"});

		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		org.junit.jupiter.api.Assertions.assertTrue(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder(), metrics));
		org.junit.jupiter.api.Assertions.assertEquals(7, metrics.getPrune());
		org.junit.jupiter.api.Assertions.assertEquals(2 * json.length(), metrics.getInputSize());

		metrics = new DefaultJsonFilterMetrics();
		org.junit.jupiter.api.Assertions.assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, new ResizableByteArrayOutputStream(128), metrics));
		org.junit.jupiter.api.Assertions.assertEquals(7, metrics.getPrune());
		org.junit.jupiter.api.Assertions.assertEquals(2 * jsonBytes.length, metrics.getInputSize());

		// output within max size, kept
		filter = new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 96, -1, null, new String[]{"//id"});

		metrics = new DefaultJsonFilterMetrics();
		org.junit.jupiter.api.Assertions.assertTrue(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder(), metrics));
		org.junit.jupiter.api.Assertions.assertEquals(5, metrics.getPrune());
		org.junit.jupiter.api.Assertions.assertEquals(json.length(), metrics.getInputSize());
		org.junit.jupiter.api.Assertions.assertEquals(96, metrics.getOutputSize());

		metrics = new DefaultJsonFilterMetrics();
		org.junit.jupiter.api.Assertions.assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, new ResizableByteArrayOutputStream(128), metrics));
		org.junit.jupiter.api.Assertions.assertEquals(5, metrics.getPrune());
		org.junit.jupiter.api.Assertions.assertEquals(jsonBytes.length, metrics.getInputSize());
		org.junit.jupiter.api.Assertions.assertEquals(96, metrics.getOutputSize());
	}

	@Test
	public void testMaxSizeMetricsOnlyWhenContentIsDropped() throws Exception {
		String pretty = "{\n  \"a\" : \"bbbbbbbbbb\",\n  \"c\" : [ 1, 2, 3 ],\n  \"d\" : { \"e\" : true }\n}";
		String compact = "{\"a\":\"bbbbbbbbbb\",\"c\":[1,2,3],\"d\":{\"e\":true}}";
		byte[] prettyBytes = pretty.getBytes(StandardCharsets.UTF_8);

		// fits once the whitespace is removed
		PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter = new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, compact.length(), -1, new String[]{"/z"}, null);

		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		StringBuilder output = new StringBuilder();
		org.junit.jupiter.api.Assertions.assertTrue(filter.process(pretty.toCharArray(), 0, pretty.length(), output, metrics));
		assertEquals(compact, output.toString());
		assertEquals(0, metrics.getMaxSize());
		assertEquals(pretty.length(), metrics.getInputSize());
		assertEquals(compact.length(), metrics.getOutputSize());

		metrics = new DefaultJsonFilterMetrics();
		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		org.junit.jupiter.api.Assertions.assertTrue(filter.process(prettyBytes, 0, prettyBytes.length, byteOutput, metrics));
		assertEquals(compact, byteOutput.toString(StandardCharsets.UTF_8));
		assertEquals(0, metrics.getMaxSize());
		assertEquals(prettyBytes.length, metrics.getInputSize());
		assertEquals(compact.length(), metrics.getOutputSize());

		// cut
		filter = new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 20, -1, new String[]{"/z"}, null);

		metrics = new DefaultJsonFilterMetrics();
		output.setLength(0);
		org.junit.jupiter.api.Assertions.assertTrue(filter.process(pretty.toCharArray(), 0, pretty.length(), output, metrics));
		assertEquals(pretty.length() - output.length(), metrics.getMaxSize());

		metrics = new DefaultJsonFilterMetrics();
		byteOutput.reset();
		org.junit.jupiter.api.Assertions.assertTrue(filter.process(prettyBytes, 0, prettyBytes.length, byteOutput, metrics));
		assertEquals(prettyBytes.length - byteOutput.size(), metrics.getMaxSize());
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
