package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilterTest extends DefaultJsonFilterTest {

	public SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, PASSTHROUGH_XPATH, FilterType.ANON)).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter filter = new SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, PASSTHROUGH_XPATH, FilterType.ANON);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter filter = new SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, PASSTHROUGH_XPATH, FilterType.ANON);
		assertNull(filter.process(TRUNCATED));
		assertNull(filter.process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void anonymize() throws Exception {
		assertThat(new SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		assertThat(new SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, DEEP_PATH1, FilterType.ANON)).hasAnonymized(DEEP_PATH1).hasAnonymizeMetrics();
	}

	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		assertThat(new SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(-1, 1, "/key1", FilterType.ANON)).hasAnonymized("/key1").hasAnonymizeMetrics();
		assertThat(new SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(-1, 1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		assertThat(new SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(-1, 2, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
	}
	
	@Test
	public void anonymizeWildcard() throws Exception {
		assertThat(new SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, DEFAULT_WILDCARD_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_WILDCARD_PATH).hasAnonymizeMetrics();
	}
	
	@Test
	public void prune() throws Exception {
		assertThat(new SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		assertThat(new SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, DEEP_PATH3, FilterType.PRUNE)).hasPruned(DEEP_PATH3).hasPruneMetrics();
	}

	@Test
	public void pruneMaxPathMatches() throws Exception {
		assertThat(new SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(-1, 1, "/key3", FilterType.PRUNE)).hasPruned("/key3").hasPruneMetrics();
		assertThat(new SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(-1, 1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		assertThat(new SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(-1, 2, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH).hasPruneMetrics();
	}

	@Test
	public void pruneWildcard() throws Exception {
		assertThat(new SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(-1, -1, DEFAULT_WILDCARD_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_WILDCARD_PATH).hasPruneMetrics();
	}

	@Test
	public void maxStringLength() throws Exception {
		assertThat(new SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, PASSTHROUGH_XPATH, FilterType.PRUNE)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH).hasMaxStringLengthMetrics();
	}

	@Test
	public void maxStringLengthMaxStringLength() throws Exception {
		assertThat(new SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, 1, "/key3", FilterType.PRUNE)).hasPruned("/key3").hasPruneMetrics();
	}
	
	@Test
	public void maxStringLengthAnonymize() throws Exception {
		assertThat(new SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, DEFAULT_PATH, FilterType.ANON))
			.hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH).hasMaxStringLengthMetrics()
			.hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
	}

	@Test
	public void maxStringLengthPrune() throws Exception {
		assertThat(new SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, DEFAULT_PATH, FilterType.PRUNE))
			.hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH).hasMaxStringLengthMetrics()
			.hasPruned(DEFAULT_PATH).hasPruneMetrics();
	}

}
