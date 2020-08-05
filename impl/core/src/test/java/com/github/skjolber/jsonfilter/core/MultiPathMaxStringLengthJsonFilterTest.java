package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class MultiPathMaxStringLengthJsonFilterTest extends DefaultJsonFilterTest {

	public MultiPathMaxStringLengthJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, -1, null, null)).hasPassthrough();
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, -1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasPassthrough();
	}
	
	@Test
	public void exception_returns_false() throws Exception {
		MultiPathMaxStringLengthJsonFilter filter = new MultiPathMaxStringLengthJsonFilter(-1, -1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ByteArrayOutputStream()));
	}	
	
	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		MultiPathMaxStringLengthJsonFilter filter = new MultiPathMaxStringLengthJsonFilter(-1, -1, null, null);
		assertNull(filter.process(TRUNCATED));
		assertFalse(filter.process(FULL, 0, FULL.length - 3, new StringBuilder()));
		
		assertFalse(filter.process(FULL, 0, FULL.length - 3, new StringBuilder()));
		assertFalse(filter.process(new String(FULL).getBytes(StandardCharsets.UTF_8), 0, FULL.length - 3, new ByteArrayOutputStream()));
	}
	
	@Test
	public void anonymize() throws Exception {
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, -1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH);
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, -1, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEFAULT_PATH);
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, -1, new String[]{DEEP_PATH1, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEEP_PATH1);
	}
	
	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, 1, new String[]{"/key1"}, null)).hasAnonymized("/key1");
	}

	@Test
	public void anonymizeWildcard() throws Exception {
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, -1, new String[]{DEFAULT_WILDCARD_PATH}, null)).hasAnonymized(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void anonymizeAny() throws Exception {
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, -1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH);
	}

	@Test
	public void prune() throws Exception {
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH);
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH})).hasPruned(DEFAULT_PATH);
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEEP_PATH3, PASSTHROUGH_XPATH})).hasPruned(DEEP_PATH3);
	}
	
	@Test
	public void pruneMaxPathMatches() throws Exception {
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, 1, null, new String[]{"/key3"})).hasPruned("/key3");
	}

	@Test
	public void pruneWildcard() throws Exception {
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEFAULT_WILDCARD_PATH})).hasPruned(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void pruneAny() throws Exception {
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, -1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH);
	}	

	@Test
	public void maxStringLength() throws Exception {
		assertThat(new MultiPathMaxStringLengthJsonFilter(DEFAULT_MAX_LENGTH, -1, null, null)).hasMaxStringLength(DEFAULT_MAX_LENGTH);
	}
	
	@Test
	public void maxStringLengthAnonymizePrune() throws Exception {
		assertThat(new MultiPathMaxStringLengthJsonFilter(DEFAULT_MAX_LENGTH, 3, new String[]{"/key1"}, new String[]{"/key3"}))
			.hasMaxStringLength(DEFAULT_MAX_LENGTH)
			.hasMaxPathMatches(3)
			.hasPruned("/key3")
			.hasAnonymized("/key1");
	}

}
