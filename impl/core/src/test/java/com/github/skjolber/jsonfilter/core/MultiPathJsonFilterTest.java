package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class MultiPathJsonFilterTest extends DefaultJsonFilterTest {

	public MultiPathJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new MultiPathJsonFilter(-1, null, null)).hasPassthrough();
		assertThat(new MultiPathJsonFilter(-1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasPassthrough();
	}
	
	@Test
	public void exception_returns_false() throws Exception {
		MultiPathJsonFilter filter = new MultiPathJsonFilter(-1, null, null);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ByteArrayOutputStream()));
	}
	
	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		MultiPathJsonFilter filter = new MultiPathJsonFilter(-1, null, null);
		
		assertNull(filter.process(TRUNCATED));
		assertNull(filter.process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
		
		assertFalse(filter.process(FULL, 0, FULL.length - 3, new StringBuilder()));
		assertFalse(filter.process(new String(FULL).getBytes(StandardCharsets.UTF_8), 0, FULL.length - 3, new ByteArrayOutputStream()));
	}

	@Test
	public void exception_incorrect_level() throws Exception {
		MultiPathJsonFilter filter = new MultiPathJsonFilter(127, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertFalse(filter.process(INCORRECT_LEVEL, new StringBuilder()));
		assertNull(filter.process(INCORRECT_LEVEL.getBytes(StandardCharsets.UTF_8)));
	}
	
	@Test
	public void anonymize() throws Exception {
		assertThat(new MultiPathJsonFilter(-1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH);
		assertThat(new MultiPathJsonFilter(-1, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEFAULT_PATH);
		assertThat(new MultiPathJsonFilter(-1, new String[]{DEEP_PATH1, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEEP_PATH1);
	}

	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		assertThat(new MultiPathJsonFilter(1, new String[]{"/key1"}, null)).hasAnonymized("/key1");
		
		assertThat(new MultiPathJsonFilter(1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH);
		assertThat(new MultiPathJsonFilter(2, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH);
	}
	
	@Test
	public void anonymizeWildcard() throws Exception {
		assertThat(new MultiPathJsonFilter(-1, new String[]{DEFAULT_WILDCARD_PATH}, null)).hasAnonymized(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void anonymizeAny() throws Exception {
		assertThat(new MultiPathJsonFilter(-1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH);
	}

	@Test
	public void prune() throws Exception {
		assertThat(new MultiPathJsonFilter(-1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH);
		assertThat(new MultiPathJsonFilter(-1, null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH})).hasPruned(DEFAULT_PATH);
		assertThat(new MultiPathJsonFilter(-1, null, new String[]{DEEP_PATH3})).hasPruned(DEEP_PATH3);
	}
	
	@Test
	public void pruneMaxPathMatches() throws Exception {
		assertThat(new MultiPathJsonFilter(1, null, new String[]{"/key3"})).hasPruned("/key3");
		
		assertThat(new MultiPathJsonFilter(1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH);
		assertThat(new MultiPathJsonFilter(2, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH);
		
	}

	@Test
	public void pruneWildcard() throws Exception {
		assertThat(new MultiPathJsonFilter(-1, null, new String[]{DEFAULT_WILDCARD_PATH})).hasPruned(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void pruneAny() throws Exception {
		assertThat(new MultiPathJsonFilter(-1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH);
	}	

	@Test
	public void anonymizePrune() throws Exception {
		assertThat(new MultiPathJsonFilter(-1, new String[]{"/key1"}, new String[]{"/key3"}))
			.hasPruned("/key3")
			.hasAnonymized("/key1");
		
		
		assertThat(new MultiFullPathJsonFilter(-1, new String[]{DEEP_PATH1}, new String[]{DEEP_PATH3}))
		.hasPruned(DEEP_PATH3)
		.hasAnonymized(DEEP_PATH1);		
	}
		
}
