package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class PathMaxStringLengthJsonFilterTest extends DefaultJsonFilterTest {

	public PathMaxStringLengthJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new PathMaxStringLengthJsonFilter(-1, -1, null, null)).hasPassthrough();
		assertThat(new PathMaxStringLengthJsonFilter(-1, -1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasPassthrough();
	}
	
	@Test
	public void exception_returns_false() throws Exception {
		PathMaxStringLengthJsonFilter filter = new PathMaxStringLengthJsonFilter(-1, -1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}	

	@Test
	public void exception_incorrect_level() throws Exception {
		PathMaxStringLengthJsonFilter filter = new PathMaxStringLengthJsonFilter(-1, 127, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertFalse(filter.process(INCORRECT_LEVEL, new StringBuilder()));
		assertNull(filter.process(INCORRECT_LEVEL.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testByteArrayRangesMaxOutputLength() throws Exception {
		// The prune message is 4 chars longer than the value it replaces. The byte[] ranges filter must
		// know the input length to tell the output length, which PathMaxSizeMaxStringLengthJsonFilter
		// uses to check whether the output fits within max size.
		String json = "{\"key\":\"value\",\"other\":\"value\"}";
		PathMaxStringLengthJsonFilter filter = new PathMaxStringLengthJsonFilter(-1, -1, null, new String[]{"/key"});
		assertEquals(json.length() + 4, filter.ranges(json.getBytes(StandardCharsets.UTF_8), 0, json.length()).getMaxOutputLength());
		assertEquals(json.length() + 4, filter.ranges(json.toCharArray(), 0, json.length()).getMaxOutputLength());
	}
	
	@Test
	public void anonymize() throws Exception {
		assertThat(new PathMaxStringLengthJsonFilter(-1, -1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH);
		assertThat(new PathMaxStringLengthJsonFilter(-1, -1, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		assertThat(new PathMaxStringLengthJsonFilter(-1, -1, new String[]{DEEP_PATH1, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEEP_PATH1).hasAnonymizeMetrics();
	}
	
	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		assertThat(new PathMaxStringLengthJsonFilter(-1, 1, new String[]{"/key1"}, null)).hasAnonymized("/key1");
		
		assertThat(new PathMaxStringLengthJsonFilter(-1, 1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		assertThat(new PathMaxStringLengthJsonFilter(-1, 2, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
	}

	@Test
	public void anonymizeWildcard() throws Exception {
		assertThat(new PathMaxStringLengthJsonFilter(-1, -1, new String[]{DEFAULT_WILDCARD_PATH}, null)).hasAnonymized(DEFAULT_WILDCARD_PATH).hasAnonymizeMetrics();
	}
	
	@Test
	public void anonymizeAny() throws Exception {
		assertThat(new PathMaxStringLengthJsonFilter(-1, -1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
	}

	@Test
	public void prune() throws Exception {
		assertThat(new PathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		assertThat(new PathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		assertThat(new PathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEEP_PATH3, PASSTHROUGH_XPATH})).hasPruned(DEEP_PATH3).hasPruneMetrics();
	}
	
	@Test
	public void pruneMaxPathMatches() throws Exception {
		assertThat(new PathMaxStringLengthJsonFilter(-1, 1, null, new String[]{"/key3"})).hasPruned("/key3").hasPruneMetrics();
		
		assertThat(new PathMaxStringLengthJsonFilter(-1, 1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		assertThat(new PathMaxStringLengthJsonFilter(-1, 2, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
	}

	@Test
	public void pruneWildcard() throws Exception {
		assertThat(new PathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEFAULT_WILDCARD_PATH})).hasPruned(DEFAULT_WILDCARD_PATH).hasPruneMetrics();
	}
	
	@Test
	public void pruneAny() throws Exception {
		assertThat(new PathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();
	}	

	@Test
	public void maxStringLength() throws Exception {
		assertThat(new PathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, null, null)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH).hasMaxStringLengthMetrics();
	}
	
	@Test
	public void maxStringLengthAnonymizePrune() throws Exception {
		assertThat(new PathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, 2, new String[]{"/key1"}, new String[]{"/key3"}))
			.hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH)
			.hasMaxPathMatches(2)
			.hasPruned("/key3").hasPruneMetrics()
			.hasAnonymized("/key1").hasAnonymizeMetrics();
	}

	@Test
	public void maxStringLengthTopLevelString() throws Exception {
		// a document which is a single string has no structural character after the string
		String string = "\"hello world, a long string\"";
		String truncated = "\"hello... + 21\"";

		PathMaxStringLengthJsonFilter filter = new PathMaxStringLengthJsonFilter(-1, -1, new String[]{DEFAULT_PATH}, new String[]{PASSTHROUGH_XPATH});
		assertEquals(string, filter.process(string));
		assertArrayEquals(string.getBytes(StandardCharsets.UTF_8), filter.process(string.getBytes(StandardCharsets.UTF_8)));

		filter = new PathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, new String[]{DEFAULT_PATH}, new String[]{PASSTHROUGH_XPATH});

		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		assertEquals(truncated, filter.process(string));
		assertEquals(truncated, filter.process(string, metrics));
		assertArrayEquals(truncated.getBytes(StandardCharsets.UTF_8), filter.process(string.getBytes(StandardCharsets.UTF_8)));
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
	public void anonymizeObjectSibling() throws Exception {
		// Triggers skipObjectMaxStringLength when non-targeted key has object value at depth > target level
		assertThat(new PathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, new String[]{"/key1"}, null)).hasAnonymized("/key1");
		assertThat(new PathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, null, new String[]{"/key1"})).hasPruned("/key1");
	}

}
