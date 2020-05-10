package com.github.skjolber.jsonfilter.core;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class MultiFullPathJsonFilterTest extends DefaultJsonFilterTest {

	public MultiFullPathJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new MultiFullPathJsonFilter(null, null)).hasPassthrough();
		assertThat(new MultiFullPathJsonFilter(new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasPassthrough();
	}
	
	@Test
	public void anonymize() throws Exception {
		assertThat(new MultiFullPathJsonFilter(new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH);
		assertThat(new MultiFullPathJsonFilter(new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEFAULT_PATH);
		assertThat(new MultiFullPathJsonFilter(new String[]{DEEP_PATH1}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEEP_PATH1);
	}

	@Test
	public void anonymizeWildcard() throws Exception {
		assertThat(new MultiFullPathJsonFilter(new String[]{DEFAULT_WILDCARD_PATH}, null)).hasAnonymized(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void prune() throws Exception {
		assertThat(new MultiFullPathJsonFilter(null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH);
		assertThat(new MultiFullPathJsonFilter(null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH})).hasPruned(DEFAULT_PATH);
		assertThat(new MultiFullPathJsonFilter(new String[]{PASSTHROUGH_XPATH}, new String[]{DEEP_PATH3})).hasPruned(DEEP_PATH3);
	}

	@Test
	public void pruneWildcard() throws Exception {
		assertThat(new MultiFullPathJsonFilter(null, new String[]{DEFAULT_WILDCARD_PATH})).hasPruned(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void AnonymizePrune() throws Exception {
		assertThat(new MultiFullPathJsonFilter(new String[]{"/key1"}, new String[]{"/key3"}))
			.hasPruned("/key3")
			.hasAnonymized("/key1");
		
		assertThat(new MultiFullPathJsonFilter(new String[]{DEEP_PATH1}, new String[]{DEEP_PATH3}))
		.hasPruned(DEEP_PATH3)
		.hasAnonymized(DEEP_PATH1);
		
	}	
	
}
