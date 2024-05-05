package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class AnyPathJsonFilterTest extends DefaultJsonFilterTest {

	public AnyPathJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new AnyPathJsonFilter(-1, new String[]{ANY_PASSTHROUGH_XPATH}, null)).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		AnyPathJsonFilter filter = new AnyPathJsonFilter(-1, new String[]{ANY_PASSTHROUGH_XPATH}, null);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}	

	@Test
	public void anonymizeAny() throws Exception {
		assertThat(new AnyPathJsonFilter(-1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
		assertThat(new AnyPathJsonFilter(-1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();
	}

	@Test
	public void pruneAny() throws Exception {
		assertThat(new AnyPathJsonFilter(-1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
		assertThat(new AnyPathJsonFilter(-1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();
	}

	@Test
	public void anonymizeAnyMaxPathMatches() throws Exception {
		assertThat(new AnyPathJsonFilter(1, new String[]{"//key1"}, null)).hasAnonymized("//key1").hasAnonymizeMetrics();
		assertThat(new AnyPathJsonFilter(2, new String[]{"//child1"}, null)).hasAnonymized("//child1").hasAnonymizeMetrics();
	}

	@Test
	public void pruneAnyMaxPathMatches() throws Exception {
		assertThat(new AnyPathJsonFilter(1, null, new String[]{"//key3"})).hasPruned("//key3").hasPruneMetrics();
	}
	
}
