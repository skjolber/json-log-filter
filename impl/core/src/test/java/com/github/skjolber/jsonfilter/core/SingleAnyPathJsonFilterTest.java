package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
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
		SingleAnyPathJsonFilter filter = new SingleAnyPathJsonFilter(-1, ANY_PASSTHROUGH_XPATH, FilterType.PRUNE);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}	

	@Test
	public void anonymizeAny() throws Exception {
		assertThat(new SingleAnyPathJsonFilter(-1, DEFAULT_ANY_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
	}

	@Test
	public void pruneAny() throws Exception {
		assertThat(new SingleAnyPathJsonFilter(-1, DEFAULT_ANY_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();
	}

	@Test
	public void anonymizeAnyMaxPathMatches() throws Exception {
		assertThat(new SingleAnyPathJsonFilter(1, "//key1", FilterType.ANON)).hasAnonymized("//key1").hasAnonymizeMetrics();
		assertThat(new SingleAnyPathJsonFilter(2, "//child1", FilterType.ANON)).hasAnonymized("//child1").hasAnonymizeMetrics();
	}

	@Test
	public void pruneAnyMaxPathMatches() throws Exception {
		assertThat(new SingleAnyPathJsonFilter(1, "//key3", FilterType.PRUNE)).hasPruned("//key3").hasPruneMetrics();
	}
	
}
