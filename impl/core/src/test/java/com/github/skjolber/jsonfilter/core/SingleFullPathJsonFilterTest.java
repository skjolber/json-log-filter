package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class SingleFullPathJsonFilterTest extends DefaultJsonFilterTest {

	public SingleFullPathJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new SingleFullPathJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.ANON)).hasPassthrough();
	}
	
	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new SingleFullPathJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.ANON).process(new char[] {}, 1, 1, new StringBuilder()));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		assertNull(new SingleFullPathJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.ANON).process(TRUNCATED));
		assertFalse(new SingleFullPathJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.ANON).process(FULL, 0, FULL.length - 3, new StringBuilder()));
	}

	@Test
	public void anonymize() throws Exception {
		assertThat(new SingleFullPathJsonFilter(-1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH);
		assertThat(new SingleFullPathJsonFilter(-1, DEEP_PATH1, FilterType.ANON)).hasAnonymized(DEEP_PATH1);
	}
	
	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		assertThat(new SingleFullPathJsonFilter(1, "/key1", FilterType.ANON)).hasAnonymized("/key1");
	}	

	@Test
	public void anonymizeWildcard() throws Exception {
		assertThat(new SingleFullPathJsonFilter(-1, DEFAULT_WILDCARD_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void prune() throws Exception {
		assertThat(new SingleFullPathJsonFilter(-1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH);
		assertThat(new SingleFullPathJsonFilter(-1, DEEP_PATH3, FilterType.PRUNE)).hasPruned(DEEP_PATH3);
	}
	
	@Test
	public void pruneMaxPathMatches() throws Exception {
		assertThat(new SingleFullPathJsonFilter(1, "/key3", FilterType.PRUNE)).hasPruned("/key3");
	}	

	@Test
	public void pruneWildcard() throws Exception {
		assertThat(new SingleFullPathJsonFilter(-1, DEFAULT_WILDCARD_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_WILDCARD_PATH);
	}

	
}
