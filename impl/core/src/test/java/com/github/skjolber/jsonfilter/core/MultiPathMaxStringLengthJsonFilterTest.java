package com.github.skjolber.jsonfilter.core;

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
	public void anonymize() throws Exception {
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, -1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH);
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, -1, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEFAULT_PATH);
		assertThat(new MultiPathMaxStringLengthJsonFilter(-1, -1, new String[]{DEEP_PATH1, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEEP_PATH1);
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
