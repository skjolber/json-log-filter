package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

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
		SingleAnyPathJsonFilter filter = new SingleAnyPathJsonFilter(-1, ANY_PASSTHROUGH_XPATH, FilterType.PRUNE);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ByteArrayOutputStream()));
	}	

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		SingleAnyPathJsonFilter filter = new SingleAnyPathJsonFilter(-1, ANY_PASSTHROUGH_XPATH, FilterType.PRUNE);
		assertNull(filter.process(TRUNCATED));
		assertFalse(filter.process(FULL, 0, FULL.length - 3, new StringBuilder()));
		
		assertFalse(filter.process(FULL, 0, FULL.length - 3, new StringBuilder()));
		assertFalse(filter.process(new String(FULL).getBytes(StandardCharsets.UTF_8), 0, FULL.length - 3, new ByteArrayOutputStream()));
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
