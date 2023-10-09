package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class SingleFullPathRemoveWhitespaceJsonFilterTest extends DefaultJsonFilterTest {

	public SingleFullPathRemoveWhitespaceJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new SingleFullPathRemoveWhitespaceJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.ANON)).hasPassthrough();
	}

	@Test
	public void exception_constructor() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			new SingleFullPathRemoveWhitespaceJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.DELETE);
		});
	}

	@Test
	public void exception_returns_false() throws Exception {
		SingleFullPathRemoveWhitespaceJsonFilter filter = new SingleFullPathRemoveWhitespaceJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.ANON);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void anonymize() throws Exception {
		assertThat(new SingleFullPathRemoveWhitespaceJsonFilter(-1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		assertThat(new SingleFullPathRemoveWhitespaceJsonFilter(-1, DEEP_PATH1, FilterType.ANON)).hasAnonymized(DEEP_PATH1).hasAnonymizeMetrics();
	}
	
	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		assertThat(new SingleFullPathRemoveWhitespaceJsonFilter(1, "/key1", FilterType.ANON)).hasAnonymized("/key1");
		
		assertThat(new SingleFullPathRemoveWhitespaceJsonFilter(1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		assertThat(new SingleFullPathRemoveWhitespaceJsonFilter(2, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
	}	

	@Test
	public void anonymizeWildcard() throws Exception {
		assertThat(new SingleFullPathRemoveWhitespaceJsonFilter(-1, DEFAULT_WILDCARD_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_WILDCARD_PATH).hasAnonymizeMetrics();
	}
	
	@Test
	public void prune() throws Exception {
		assertThat(new SingleFullPathRemoveWhitespaceJsonFilter(-1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		assertThat(new SingleFullPathRemoveWhitespaceJsonFilter(-1, DEEP_PATH3, FilterType.PRUNE)).hasPruned(DEEP_PATH3).hasPruneMetrics();
	}
	
	@Test
	public void pruneMaxPathMatches() throws Exception {
		assertThat(new SingleFullPathRemoveWhitespaceJsonFilter(1, "/key3", FilterType.PRUNE)).hasPruned("/key3");
		
		assertThat(new SingleFullPathRemoveWhitespaceJsonFilter(1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		assertThat(new SingleFullPathRemoveWhitespaceJsonFilter(2, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH).hasPruneMetrics();
	}	

	@Test
	public void pruneWildcard() throws Exception {
		assertThat(new SingleFullPathRemoveWhitespaceJsonFilter(-1, DEFAULT_WILDCARD_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_WILDCARD_PATH).hasPruneMetrics();
	}

	
	@Test
	public void test() {
		String string = "{\"a\":{\"b\":{\"key\":\"d\\\"a\"}}}";
		
		System.out.println("Input size is " + string.length());
		
		int size = 26;
		
		SingleFullPathRemoveWhitespaceJsonFilter filter = new SingleFullPathRemoveWhitespaceJsonFilter(-1, DEFAULT_PATH, FilterType.ANON);
		
		System.out.println("Original:");
		System.out.println(string);
		System.out.println("Filtered:");

		String filtered = filter.process(string);
		System.out.println(filtered);
		
		byte[] filteredBytes = filter.process(string.getBytes());
		System.out.println(new String(filteredBytes));
		
		System.out.println(filtered.length());

	}
	
}
