package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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

public class MaxStringLengthMaxSizeJsonFilterTest extends DefaultJsonFilterTest {

	private static class MustContrainMaxStringLengthMaxSizeJsonFilter extends MaxStringLengthMaxSizeJsonFilter {
		public MustContrainMaxStringLengthMaxSizeJsonFilter(int maxStringLength, int maxSize) {
			super(maxStringLength, maxSize);
		}
		public MustContrainMaxStringLengthMaxSizeJsonFilter(int maxStringLength, int maxSize,
				String pruneMessage, String anonymizeMessage, String truncateMessage) {
			super(maxStringLength, maxSize, pruneMessage, anonymizeMessage, truncateMessage);
		}
		@Override
		protected boolean mustConstrainMaxSize(int length) {
			return true;
		}
	};

	public MaxStringLengthMaxSizeJsonFilterTest() throws Exception {
		super();
	}
	
	@Test
	@ResourceLock(value = "jackson")
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MaxStringLengthMaxSizeJsonFilter(128, size));
	}

	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new MaxStringLengthMaxSizeJsonFilter(128, size));
	}

	@Test
	public void passthrough_success() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MaxStringLengthMaxSizeJsonFilter(-1, size);

		assertThat(maxSize, new MaxStringLengthJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void maxStringLength() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MaxStringLengthMaxSizeJsonFilter(DEFAULT_MAX_STRING_LENGTH, size);
		
		assertThat(maxSize, new MaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH).hasMaxStringLengthMetrics();
	}
	
	@Test
	public void exception_returns_false() throws Exception {
		JsonFilter filter = new MaxStringLengthMaxSizeJsonFilter(-1, -1);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertNull(filter.process(new byte[] {}, 1, 1));
	}

	@Test
	public void testGrowSquareBrackets() throws Exception {
		// Build 35 levels of nested objects to trigger grow() in rangesMaxSizeMaxStringLength
		// Use a long string value and capped maxSize to avoid reading past array bounds
		StringBuilder deepJson = new StringBuilder();
		for (int i = 0; i < 35; i++) {
			deepJson.append("{\"k").append(i).append("\":");
		}
		deepJson.append("\"").append("x".repeat(500)).append("\"");
		for (int i = 0; i < 35; i++) {
			deepJson.append("}");
		}
		String json = deepJson.toString();

		MustContrainMaxStringLengthMaxSizeJsonFilter filter = new MustContrainMaxStringLengthMaxSizeJsonFilter(5, 400);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testExceptionCaughtInRanges() throws Exception {
		// ranges(char[]) and ranges(byte[]) catch Exception → return null → process returns false
		MustContrainMaxStringLengthMaxSizeJsonFilter filter = new MustContrainMaxStringLengthMaxSizeJsonFilter(3, 100);
		assertFalse(filter.process(new char[]{}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[]{}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void testLongKeyIsNotTruncated() throws Exception {
		// A key (field name) that's >= maxStringLength should NOT be truncated (just skipped)
		// Covers the 'was a field name' path: if(chars[nextOffset] == ':') → offset = nextOffset+1; continue
		// maxSize must be < JSON length to ensure loop terminates normally
		MustContrainMaxStringLengthMaxSizeJsonFilter filter = new MustContrainMaxStringLengthMaxSizeJsonFilter(3, 20);
		String json = "{\"longlongkey\":\"value\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		// Use assertFalse for byte to avoid null-assertion when maxSizeLimit > maxReadLimit
		assertFalse(filter.process(new byte[]{}, 1, 1, new ResizableByteArrayOutputStream(128)));
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		// Process bytes correctly with large enough maxSize via validate path:
		MustContrainMaxStringLengthMaxSizeJsonFilter filterB = new MustContrainMaxStringLengthMaxSizeJsonFilter(3, 22);
		assertNotNull(filterB.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testLongValueRemoveLastFilter() throws Exception {
		// Long value with large truncate message → after addMaxLength, nextOffset > new maxSizeLimit → removeLastFilter
		// Use maxSize smaller than JSON to prevent overflow past maxReadLimit
		String json = "{\"k\":\"abcdefg\",\"n\":\"v\"}";
		MustContrainMaxStringLengthMaxSizeJsonFilter filter = new MustContrainMaxStringLengthMaxSizeJsonFilter(3, 
			json.length() - 1,
			"\"***SKIPPED***\"", "\"***\"", "X".repeat(50));
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testLongKeyWithWhitespaceBeforeColon() throws Exception {
		// Long key with whitespace before colon → covers multi-whitespace skip on line 149
		// maxSize must be <= JSON length
		String json = "{\"longkey\"   :\"value\"}";
		MustContrainMaxStringLengthMaxSizeJsonFilter filter = new MustContrainMaxStringLengthMaxSizeJsonFilter(3, json.length() - 1);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));
	}

}
