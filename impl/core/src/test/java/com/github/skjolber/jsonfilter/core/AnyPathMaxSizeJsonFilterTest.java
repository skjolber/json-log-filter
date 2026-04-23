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
		// Build 35 levels of nested objects to trigger grow() in rangesAnyPathMaxSize
		// Use long string + capped maxSize to avoid reading past array bounds
		StringBuilder deepJson = new StringBuilder();
		for (int i = 0; i < 35; i++) {
			deepJson.append("{\"k").append(i).append("\":");
		}
		deepJson.append("\"").append("x".repeat(500)).append("\"");
		for (int i = 0; i < 35; i++) {
			deepJson.append("}");
		}
		String json = deepJson.toString();

		// Use //targetKey (any-path) so anyPathFilters != null, preventing early skip of nested objects
		MustContrainAnyPathMaxSizeJsonFilter filter = new MustContrainAnyPathMaxSizeJsonFilter(400, -1, new String[]{"//targetKey"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
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
		// maxSize=10 → after '{' maxSizeLimit=9. //k matches. nextOffset=5.
		// Default pruneMessage (~13 chars). 5+13 > 9 → break loop.
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
		// After prune, offset > maxSizeLimit → removeLastFilter
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
		// After anon, offset > maxSizeLimit → removeLastFilter
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
		// mustConstrainMaxSize returns false → calls super.ranges() → covers mustConstrainMaxSize false branch
		AnyPathMaxSizeJsonFilter filter = new AnyPathMaxSizeJsonFilter(100000, -1, new String[]{"//k"}, null);
		String json = "{\"k\":\"value\"}";
		// length=13 is much less than maxSize=100000 → mustConstrainMaxSize returns false
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testExceptionInRangesReturnsFalse() throws Exception {
		// catch(Exception) in ranges() → return null → process() returns false
		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(100, -1, new String[]{"//k"}, null);
		assertFalse(filter.process(new char[]{}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[]{}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void testPathMatchesExhaustedLargeMaxSize() throws Exception {
		// pathMatches=1, large maxSize >> JSON length → after match, pathMatches=0 AND maxSizeLimit >= maxReadLimit
		// Covers: filter.setLevel(0); return filter (lines 208-209 char / 483-484 byte)
		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(500, 1, new String[]{"//k"}, null);
		String json = "{\"k\":\"v1\",\"k\":\"v2\",\"other\":\"data\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testPathMatchesExhaustedSmallMaxSize() throws Exception {
		// pathMatches=1, small maxSize < JSON length → after match, pathMatches=0, maxSizeLimit < maxReadLimit
		// Covers: rangesMaxSize (line 216 char / 491 byte)
		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(15, 1, new String[]{"//k"}, null);
		String json = "{\"k\":\"v\",\"other\":\"data and more data\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testMaxSizeLimitReachedAfterMatch() throws Exception {
		// After match, maxSizeLimit >= maxReadLimit → rangesAnyPath (lines 223-226 char)
		// This happens when removing a long value makes maxSizeLimit cover rest of document
		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(500, -1, new String[]{"//k"}, null);
		// large maxSize so after matching and removing, maxSizeLimit >= maxReadLimit
		String json = "{\"k\":\"longlonglongvalue\",\"other\":\"data\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testRangesMaxSizeBreakLoopOnOpenBracket() throws Exception {
		// rangesMaxSize: [ or { where offset >= maxSizeLimit after decrement → break loop (line 273 char, 545 byte)
		// Need pathMatches=1 to call rangesMaxSize with the remaining JSON
		// JSON = {"k":"verylongvalueXX","n":[1]}, maxSize=23:
		//   After prune of "k": maxSizeLimit=22+6=28, rangesMaxSize starts at 5
		//   '[' at 27: maxSizeLimit--=27, 27>=27 → break loop at line 273
		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(23, 1, null, new String[]{"//k"});
		String json = "{\"k\":\"verylongvalueXX\",\"n\":[1]}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testRangesMaxSizeGrowSquareBrackets() throws Exception {
		// rangesMaxSize: 35 levels of nesting in remaining JSON → grow() (line 280 char, 552 byte)
		// pathMatches=1, after first match, rangesMaxSize processes deeply nested remaining JSON
		StringBuilder deepJson = new StringBuilder();
		deepJson.append("{\"k\":\"v\",\"deep\":");
		for (int i = 0; i < 35; i++) {
			deepJson.append("{\"n").append(String.format("%02d", i)).append("\":");
		}
		deepJson.append("1");
		for (int i = 0; i < 35; i++) {
			deepJson.append("}");
		}
		deepJson.append("}");
		String json = deepJson.toString();

		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(json.length() + 100, 1, new String[]{"//k"}, null);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testAnonBreakLoopMessageTooLarge() throws Exception {
		// ANON case: nextOffset + anonMsg.length > maxSizeLimit → break loop (line 166 char, 441 byte)
		// anonMsg = '"*"' = 3 chars. With maxSize=8: after '{', maxSizeLimit=7.
		// "k" value at nextOffset=5: 5+3=8 > 7 → break loop
		MustContrainAnyPathMaxSizeJsonFilter filter =
			new MustContrainAnyPathMaxSizeJsonFilter(8, -1, new String[]{"//k"}, null);
		String json = "{\"k\":\"val\",\"other\":\"v\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

}
