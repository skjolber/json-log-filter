package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class SingleAnyPathJsonFilterTest extends DefaultJsonFilterTest {

	public SingleAnyPathJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new SingleAnyPathJsonFilter(-1, ANY_PASSTHROUGH_XPATH, FilterType.PRUNE)).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new SingleAnyPathJsonFilter(-1, ANY_PASSTHROUGH_XPATH, FilterType.PRUNE).process(new char[] {}, 1, 1, new StringBuilder()));
	}	

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		assertNull(new SingleAnyPathJsonFilter(-1, ANY_PASSTHROUGH_XPATH, FilterType.PRUNE).process(TRUNCATED));
		assertFalse(new SingleAnyPathJsonFilter(-1, ANY_PASSTHROUGH_XPATH, FilterType.PRUNE).process(FULL, 0, FULL.length - 3, new StringBuilder()));
	}
	
	@Test
	public void anonymizeAny() throws Exception {
		assertThat(new SingleAnyPathJsonFilter(-1, DEFAULT_ANY_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_ANY_PATH);
	}

	@Test
	public void pruneAny() throws Exception {
		assertThat(new SingleAnyPathJsonFilter(-1, DEFAULT_ANY_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_ANY_PATH);
	}

	@Test
	public void anonymizeAnyMaxPathMatches() throws Exception {
		assertThat(new SingleAnyPathJsonFilter(1, "//key1", FilterType.ANON)).hasAnonymized("//key1");
	}

	@Test
	public void pruneAnyMaxPathMatches() throws Exception {
		assertThat(new SingleAnyPathJsonFilter(1, "//key3", FilterType.PRUNE)).hasPruned("//key3");
	}
	
}
