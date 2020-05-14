package com.github.skjolber.jsonfilter.core;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class SingleAnyPathMaxStringLengthJsonFilterTest extends DefaultJsonFilterTest {

	public SingleAnyPathMaxStringLengthJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new SingleAnyPathMaxStringLengthJsonFilter(-1, -1, ANY_PASSTHROUGH_XPATH, FilterType.PRUNE)).hasPassthrough();
	}

	@Test
	public void anonymizeAny() throws Exception {
		assertThat(new SingleAnyPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_ANY_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_ANY_PATH);
	}

	@Test
	public void pruneAny() throws Exception {
		assertThat(new SingleAnyPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_ANY_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_ANY_PATH);
	}	
	
}
