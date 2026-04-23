package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class PathMaxStringLengthRemoveWhitespaceJsonFilterTest extends DefaultJsonFilterTest {

	public PathMaxStringLengthRemoveWhitespaceJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, null, null)).hasPassthrough();
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasPassthrough();
	}
	
	@Test
	public void exception_returns_false() throws Exception {
		PathMaxStringLengthRemoveWhitespaceJsonFilter filter = new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}
	
	@Test
	public void anonymize() throws Exception {
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH);
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, new String[]{DEEP_PATH1, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEEP_PATH1).hasAnonymizeMetrics();
	}
	
	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, 1, new String[]{"/key1"}, null)).hasAnonymized("/key1");
		
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, 1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, 2, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
	}

	@Test
	public void anonymizeWildcard() throws Exception {
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, new String[]{DEFAULT_WILDCARD_PATH}, null)).hasAnonymized(DEFAULT_WILDCARD_PATH).hasAnonymizeMetrics();
	}
	
	@Test
	public void anonymizeAny() throws Exception {
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
	}

	@Test
	public void prune() throws Exception {
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, null, new String[]{DEEP_PATH3, PASSTHROUGH_XPATH})).hasPruned(DEEP_PATH3).hasPruneMetrics();
	}
	
	@Test
	public void pruneMaxPathMatches() throws Exception {
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, 1, null, new String[]{"/key3"})).hasPruned("/key3").hasPruneMetrics();
		
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, 1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, 2, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
	}

	@Test
	public void pruneWildcard() throws Exception {
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, null, new String[]{DEFAULT_WILDCARD_PATH})).hasPruned(DEFAULT_WILDCARD_PATH).hasPruneMetrics();
	}
	
	@Test
	public void pruneAny() throws Exception {
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();
	}

	@Test
	public void maxStringLength() throws Exception {
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, null, null)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH).hasMaxStringLengthMetrics();
	}
	
	@Test
	public void maxStringLengthAnonymizePrune() throws Exception {
		assertThat(new PathMaxStringLengthRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, 2, new String[]{"/key1"}, new String[]{"/key3"}))
			.hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH)
			.hasMaxPathMatches(2)
			.hasPruned("/key3").hasPruneMetrics()
			.hasAnonymized("/key1").hasAnonymizeMetrics();
	}

	@Test
	public void testSkipObjectWithWhitespaceBeforeColon() throws Exception {
		// Tests skipObjectMaxStringLength with whitespace before colon inside a skipped object
		// /k path at level 1 with nested JSON containing whitespace-before-colon keys
		PathMaxStringLengthRemoveWhitespaceJsonFilter filter =
			new PathMaxStringLengthRemoveWhitespaceJsonFilter(3, -1, null, new String[]{"/k"});
		String json = "{\"k\":{\"inner key\"  :  \"longlongvalue\",\"b\":\"x\"},\"other\":\"data\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testSkipObjectWithLongValues() throws Exception {
		// skipObjectMaxStringLength with long values that exceed maxStringLength
		// The object being skipped has a value longer than maxStringLength=3
		PathMaxStringLengthRemoveWhitespaceJsonFilter filter =
			new PathMaxStringLengthRemoveWhitespaceJsonFilter(3, -1, new String[]{"/k"}, null);
		String json = "{\"k\":{\"inner\":\"longlonglong\"},\"other\":\"more\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testSkipNestedObjectWithMaxStringLength() throws Exception {
		// Deep nesting: level > pathItem.getLevel() → skipObjectMaxStringLength called
		// The nested object also has whitespace and long values
		PathMaxStringLengthRemoveWhitespaceJsonFilter filter =
			new PathMaxStringLengthRemoveWhitespaceJsonFilter(5, -1, new String[]{"/top"}, null);
		String json = "{\"top\":\"value\",\"other\":{\"deep\":\"longlongvalue\",\"k2\":\"x\"}}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testSkipObjectWithWhitespaceAfterColon() throws Exception {
		// skipObjectMaxStringLength: long key without whitespace before colon
		// but with whitespace AFTER colon → covers lines 251-261 (char) / 251-261 (byte)
		// Need level > pathItem.getLevel() to trigger skipObjectMaxStringLength
		// Path /k at level 1, JSON has 3 levels: {k:v, b:{c:{longlongkey: "value"}}}
		PathMaxStringLengthRemoveWhitespaceJsonFilter filter =
			new PathMaxStringLengthRemoveWhitespaceJsonFilter(3, -1, new String[]{"/k"}, null);
		// "longlongkey": "value" - no whitespace before colon, whitespace after colon
		String json = "{\"k\":\"v\",\"b\":{\"c\":{\"longlongkey\":  \"value\"}}}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testAnonymizeObjectOrArrayWithLargeMaxSize() throws Exception {
		// anonymizeObjectOrArrayMaxSize/skipObjectOrArrayMaxSizeMaxStringLength:
		// Anonymize an object value at level > pathItem.getLevel() with deep nesting
		PathMaxStringLengthRemoveWhitespaceJsonFilter filter =
			new PathMaxStringLengthRemoveWhitespaceJsonFilter(3, -1, new String[]{"/k"}, null);
		// deep nesting inside the non-path object to trigger long key paths
		String json = "{\"k\":\"v\",\"outer\":{\"inner\":{\"longlongkey\":  \"val\",\"longlongkey2\":\"v2\"}}}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testAnonObjectValueWithInternalWhitespace() throws Exception {
		// CharArrayWhitespaceFilter.anonymizeObjectOrArray line 74 (char):
		// and ByteArrayWhitespaceFilter line 99 (byte):
		// the break for whitespace chars (' ', '\t', '\n', '\r') inside an anonymized object
		// Need JSON with whitespace inside the matched key's object value
		PathMaxStringLengthRemoveWhitespaceJsonFilter filter =
			new PathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, new String[]{"/k"}, null);
		String json = "{\"k\":{ \"a\": \"value\" , \"b\" : 1 }}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

}
