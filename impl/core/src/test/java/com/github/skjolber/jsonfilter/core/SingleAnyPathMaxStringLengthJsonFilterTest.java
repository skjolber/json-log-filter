package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

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
	public void exception_returns_false() throws Exception {
		SingleAnyPathMaxStringLengthJsonFilter filter = new SingleAnyPathMaxStringLengthJsonFilter(-1, -1, ANY_PASSTHROUGH_XPATH, FilterType.PRUNE);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ByteArrayOutputStream()));
	}	
	
	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		SingleAnyPathMaxStringLengthJsonFilter filter = new SingleAnyPathMaxStringLengthJsonFilter(-1, -1, ANY_PASSTHROUGH_XPATH, FilterType.PRUNE);
		assertNull(filter.process(TRUNCATED));
		assertFalse(filter.process(FULL, 0, FULL.length - 3, new StringBuilder()));
		
		assertFalse(filter.process(FULL, 0, FULL.length - 3, new StringBuilder()));
		assertFalse(filter.process(new String(FULL).getBytes(StandardCharsets.UTF_8), 0, FULL.length - 3, new ByteArrayOutputStream()));
	}

	@Test
	public void anonymizeAny() throws Exception {
		assertThat(new SingleAnyPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_ANY_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_ANY_PATH);
	}

	@Test
	public void pruneAny() throws Exception {
		assertThat(new SingleAnyPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_ANY_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_ANY_PATH);
	}	

	@Test
	public void anonymizeAnyMaxStringLength() throws Exception {
		assertThat(new SingleAnyPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, "//key1", FilterType.ANON)).hasAnonymized("//key1");
	}

	@Test
	public void anonymizeAnyMaxStringLengthMaxPathMatches() throws Exception {
		assertThat(new SingleAnyPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, 1, "//key1", FilterType.ANON)).hasAnonymized("//key1");
		assertThat(new SingleAnyPathMaxStringLengthJsonFilter(4, 1, "//child1", FilterType.ANON)).hasAnonymized("//child1");
		assertThat(new SingleAnyPathMaxStringLengthJsonFilter(4, 2, "//child1", FilterType.ANON)).hasAnonymized("//child1");
	}

	@Test
	public void pruneAnyMaxStringLength() throws Exception {
		assertThat(new SingleAnyPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, "//key3", FilterType.PRUNE)).hasPruned("//key3");
	}		
	
	@Test
	public void pruneAnyMaxStringLengthMaxPathMatches() throws Exception {
		assertThat(new SingleAnyPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, 1, "//key3", FilterType.PRUNE)).hasPruned("//key3");
	}
}
