package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;

public class PathJsonFilterTest extends DefaultJsonFilterTest {

	/**
	 * A subclass that calls the 1-param getCharArrayRangesFilter(int) method
	 * to cover the otherwise-unreachable method.
	 */
	private static class TestAbstractRangesPathJsonFilter extends AbstractRangesPathJsonFilter {
		public TestAbstractRangesPathJsonFilter() {
			super(-1, -1, -1, new String[]{"/key"}, null, "p", "a", "t");
		}
		@Override
		protected CharArrayRangesFilter ranges(char[] chars, int offset, int length) {
			return getCharArrayRangesFilter(length); // calls the 1-param version
		}
		@Override
		protected ByteArrayRangesFilter ranges(byte[] chars, int offset, int length) {
			return null;
		}
	}

	public PathJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void testGetCharArrayRangesFilterOneParam() {
		// Covers AbstractRangesPathJsonFilter.getCharArrayRangesFilter(int)
		TestAbstractRangesPathJsonFilter filter = new TestAbstractRangesPathJsonFilter();
		StringBuilder sb = new StringBuilder();
		filter.process("{\"key\":\"value\"}".toCharArray(), 0, 15, sb);
		assertNotNull(sb.toString());
	}

	@Test
	public void exception_returns_false_with_metrics() throws Exception {
		// AbstractRangesPathJsonFilter.process(char/byte, ..., JsonFilterMetrics) returns false on exception
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		FullPathJsonFilter filter = new FullPathJsonFilter(-1, new String[]{"/key"}, null);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder(), metrics));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128), metrics));
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new PathJsonFilter(-1, null, null)).hasPassthrough();
		assertThat(new PathJsonFilter(-1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasPassthrough();
	}
	
	@Test
	public void exception_returns_false() throws Exception {
		PathJsonFilter filter = new PathJsonFilter(-1, null, null);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void exception_incorrect_level() throws Exception {
		PathJsonFilter filter = new PathJsonFilter(127, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertFalse(filter.process(INCORRECT_LEVEL, new StringBuilder()));
		assertNull(filter.process(INCORRECT_LEVEL.getBytes(StandardCharsets.UTF_8)));
	}
	
	@Test
	public void anonymize() throws Exception {
		assertThat(new PathJsonFilter(-1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		assertThat(new PathJsonFilter(-1, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();;
		assertThat(new PathJsonFilter(-1, new String[]{DEEP_PATH1, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEEP_PATH1).hasAnonymizeMetrics();;
		
		assertThat(new PathJsonFilter(-1, new String[]{"/key1", "/key2", "/key3"}, null)).hasAnonymized("/key1", "/key2", "/key3").hasAnonymizeMetrics();;
		assertThat(new PathJsonFilter(-1, new String[]{"/xxkey2", "/xxkey3", "/key1"}, null)).hasAnonymized("/xxkey2", "/xxkey3", "/key1").hasAnonymizeMetrics();;
	}

	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		assertThat(new PathJsonFilter(1, new String[]{"/key1"}, null)).hasAnonymized("/key1").hasAnonymizeMetrics();;
		
		assertThat(new PathJsonFilter(1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();;
		assertThat(new PathJsonFilter(2, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();;
	}
	
	@Test
	public void anonymizeWildcard() throws Exception {
		assertThat(new PathJsonFilter(-1, new String[]{DEFAULT_WILDCARD_PATH}, null)).hasAnonymized(DEFAULT_WILDCARD_PATH).hasAnonymizeMetrics();;
	}
	
	@Test
	public void anonymizeAny() throws Exception {
		assertThat(new PathJsonFilter(-1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();;
	}

	@Test
	public void prune() throws Exception {
		assertThat(new PathJsonFilter(-1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		assertThat(new PathJsonFilter(-1, null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		assertThat(new PathJsonFilter(-1, null, new String[]{DEEP_PATH3})).hasPruned(DEEP_PATH3).hasPruneMetrics();
	}
	
	@Test
	public void pruneMaxPathMatches() throws Exception {
		assertThat(new PathJsonFilter(1, null, new String[]{"/key3"})).hasPruned("/key3").hasPruneMetrics();
		
		assertThat(new PathJsonFilter(1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		assertThat(new PathJsonFilter(2, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		
	}

	@Test
	public void pruneWildcard() throws Exception {
		assertThat(new PathJsonFilter(-1, null, new String[]{DEFAULT_WILDCARD_PATH})).hasPruned(DEFAULT_WILDCARD_PATH).hasPruneMetrics();
	}
	
	@Test
	public void pruneAny() throws Exception {
		assertThat(new PathJsonFilter(-1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();
	}	

	@Test
	public void anonymizePrune() throws Exception {
		assertThat(new PathJsonFilter(-1, new String[]{"/key1"}, new String[]{"/key3"}))
			.hasPruned("/key3")
			.hasAnonymized("/key1");
				
		assertThat(new FullPathJsonFilter(-1, new String[]{DEEP_PATH1}, new String[]{DEEP_PATH3}))
		.hasPruned(DEEP_PATH3).hasPruneMetrics()
		.hasAnonymized(DEEP_PATH1).hasAnonymizeMetrics();		
	}

}
