package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class MultiFullPathJsonFilterTest extends DefaultJsonFilterTest {

	public MultiFullPathJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void testConstructor() throws Exception {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new MultiFullPathJsonFilter(-1, new String[]{"//abc"}, null);
		});
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new MultiFullPathJsonFilter(-1, null, null)).hasPassthrough();
		assertThat(new MultiFullPathJsonFilter(-1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		MultiFullPathJsonFilter filter = new MultiFullPathJsonFilter(-1, null, null);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void exception_incorrect_level() throws Exception {
		MultiFullPathJsonFilter filter = new MultiFullPathJsonFilter(127, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertFalse(filter.process(INCORRECT_LEVEL, new StringBuilder()));
		assertNull(filter.process(INCORRECT_LEVEL.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void anonymize() throws Exception {
		assertThat(new MultiFullPathJsonFilter(-1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		assertThat(new MultiFullPathJsonFilter(-1, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		assertThat(new MultiFullPathJsonFilter(-1, new String[]{DEEP_PATH1}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEEP_PATH1).hasAnonymizeMetrics();
	}

	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		assertThat(new MultiFullPathJsonFilter(1, new String[]{"/key1"}, null)).hasAnonymized("/key1");

		assertThat(new MultiFullPathJsonFilter(1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		assertThat(new MultiFullPathJsonFilter(2, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();

	}

	@Test
	public void anonymizeWildcard() throws Exception {
		assertThat(new MultiFullPathJsonFilter(-1, new String[]{DEFAULT_WILDCARD_PATH}, null)).hasAnonymized(DEFAULT_WILDCARD_PATH).hasAnonymizeMetrics();
	}

	@Test
	public void prune() throws Exception {
		assertThat(new MultiFullPathJsonFilter(-1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		assertThat(new MultiFullPathJsonFilter(-1, null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		assertThat(new MultiFullPathJsonFilter(-1, new String[]{PASSTHROUGH_XPATH}, new String[]{DEEP_PATH3})).hasPruned(DEEP_PATH3).hasPruneMetrics();
	}

	@Test
	public void pruneMaxPathMatches() throws Exception {
		assertThat(new MultiFullPathJsonFilter(1, null, new String[]{"/key3"})).hasPruned("/key3");

		assertThat(new MultiFullPathJsonFilter(1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		assertThat(new MultiFullPathJsonFilter(2, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
	}

	@Test
	public void pruneWildcard() throws Exception {
		assertThat(new MultiFullPathJsonFilter(-1, null, new String[]{DEFAULT_WILDCARD_PATH})).hasPruned(DEFAULT_WILDCARD_PATH).hasPruneMetrics();
	}

	@Test
	public void AnonymizePrune() throws Exception {
		assertThat(new MultiFullPathJsonFilter(-1, new String[]{"/key1"}, new String[]{"/key3"}))
		.hasPruned("/key3")
		.hasAnonymized("/key1");

		assertThat(new MultiFullPathJsonFilter(-1, new String[]{DEEP_PATH1}, new String[]{DEEP_PATH3}))
		.hasPruned(DEEP_PATH3).hasPruneMetrics()
		.hasAnonymized(DEEP_PATH1).hasAnonymizeMetrics();

	}	

}
