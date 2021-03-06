package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class SingleFullPathMaxStringLengthJsonFilterTest extends DefaultJsonFilterTest {

	public SingleFullPathMaxStringLengthJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new SingleFullPathMaxStringLengthJsonFilter(-1, -1, PASSTHROUGH_XPATH, FilterType.ANON)).hasPassthrough();
	}
	
	@Test
	public void exception_returns_false() throws Exception {
		SingleFullPathMaxStringLengthJsonFilter filter = new SingleFullPathMaxStringLengthJsonFilter(-1, -1, PASSTHROUGH_XPATH, FilterType.ANON);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ByteArrayOutputStream()));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		SingleFullPathMaxStringLengthJsonFilter filter = new SingleFullPathMaxStringLengthJsonFilter(-1, -1, PASSTHROUGH_XPATH, FilterType.ANON);
		assertNull(filter.process(TRUNCATED));
		assertFalse(filter.process(FULL, 0, FULL.length - 3, new StringBuilder()));
		
		assertFalse(filter.process(FULL, 0, FULL.length - 3, new StringBuilder()));
		assertFalse(filter.process(new String(FULL).getBytes(StandardCharsets.UTF_8), 0, FULL.length - 3, new ByteArrayOutputStream()));
	}

	@Test
	public void exception_incorrect_level() throws Exception {
		SingleFullPathMaxStringLengthJsonFilter filter = new SingleFullPathMaxStringLengthJsonFilter(-1, 127, PASSTHROUGH_XPATH, FilterType.ANON);
		assertFalse(filter.process(INCORRECT_LEVEL, new StringBuilder()));
		assertNull(filter.process(INCORRECT_LEVEL.getBytes(StandardCharsets.UTF_8)));
	}
	
	@Test
	public void anonymize() throws Exception {
		assertThat(new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH);
		assertThat(new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEEP_PATH1, FilterType.ANON)).hasAnonymized(DEEP_PATH1);
	}

	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		assertThat(new SingleFullPathMaxStringLengthJsonFilter(-1, 1, "/key1", FilterType.ANON)).hasAnonymized("/key1");
		assertThat(new SingleFullPathMaxStringLengthJsonFilter(-1, 1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH);
		assertThat(new SingleFullPathMaxStringLengthJsonFilter(-1, 2, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH);
	}
	
	@Test
	public void anonymizeWildcard() throws Exception {
		assertThat(new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_WILDCARD_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void prune() throws Exception {
		assertThat(new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH);
		assertThat(new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEEP_PATH3, FilterType.PRUNE)).hasPruned(DEEP_PATH3);
	}

	@Test
	public void pruneMaxPathMatches() throws Exception {
		assertThat(new SingleFullPathMaxStringLengthJsonFilter(-1, 1, "/key3", FilterType.PRUNE)).hasPruned("/key3");
		assertThat(new SingleFullPathMaxStringLengthJsonFilter(-1, 1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH);
		assertThat(new SingleFullPathMaxStringLengthJsonFilter(-1, 2, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH);
	}
	
	@Test
	public void pruneWildcard() throws Exception {
		assertThat(new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_WILDCARD_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_WILDCARD_PATH);
	}

	@Test
	public void maxStringLength() throws Exception {
		assertThat(new SingleFullPathMaxStringLengthJsonFilter(DEFAULT_MAX_LENGTH, -1, PASSTHROUGH_XPATH, FilterType.PRUNE)).hasMaxStringLength(DEFAULT_MAX_LENGTH);
	}

	@Test
	public void maxStringLengthMaxStringLength() throws Exception {
		assertThat(new SingleFullPathMaxStringLengthJsonFilter(DEFAULT_MAX_LENGTH, 1, "/key3", FilterType.PRUNE)).hasPruned("/key3");
	}

}
