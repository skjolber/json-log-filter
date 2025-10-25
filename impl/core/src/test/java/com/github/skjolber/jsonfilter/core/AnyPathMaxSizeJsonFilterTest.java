package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair.MaxSizeJsonFilterFunction;

public class AnyPathMaxSizeJsonFilterTest extends DefaultJsonFilterTest {

	public AnyPathMaxSizeJsonFilterTest() throws Exception {
		super();
	}

	@Test
	@ResourceLock(value = "jackson")
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new AnyPathMaxSizeJsonFilter(size, -1, null, new String[] {"//CVE_data_meta"}));
	}
	
	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new AnyPathMaxSizeJsonFilter(size, -1, new String[] {"/CVE_Items/cve/CVE_data_meta"}, null));
	}
	
	@Test
	public void passthrough_success() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new AnyPathMaxSizeJsonFilter(size, -1, null, null);
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, -1, null, null)).hasPassthrough();
		
		maxSize = (size) -> new AnyPathMaxSizeJsonFilter(size, -1, new String[]{ANY_PASSTHROUGH_XPATH}, new String[]{ANY_PASSTHROUGH_XPATH});
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, -1, new String[]{ANY_PASSTHROUGH_XPATH}, new String[]{ANY_PASSTHROUGH_XPATH})).hasPassthrough();
	}
	
	@Test
	public void anonymize() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new AnyPathMaxSizeJsonFilter(size, -1, new String[]{DEFAULT_ANY_PATH}, null);
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, -1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
	}
	
	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new AnyPathMaxSizeJsonFilter(size, 1, new String[]{DEFAULT_ANY_PATH}, null);
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, 1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
		
		maxSize = (size) -> new AnyPathMaxSizeJsonFilter(size, 2, new String[]{DEFAULT_ANY_PATH}, null);
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, 2, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
	}

	@Test
	public void prune() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new AnyPathMaxSizeJsonFilter(size, -1, null, new String[]{DEFAULT_ANY_PATH});
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, -1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();
	}
	
	@Test
	public void pruneMaxPathMatches() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new AnyPathMaxSizeJsonFilter(size, 1, null, new String[]{DEFAULT_ANY_PATH});
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, 1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();
		
		maxSize = (size) -> new AnyPathMaxSizeJsonFilter(size, 2, null, new String[]{DEFAULT_ANY_PATH});
		assertThat(maxSize, new AnyPathMaxSizeJsonFilter(-1, 2, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();
	}

	@Test
	public void exception_returns_false() throws Exception {
		AnyPathMaxSizeJsonFilter filter = new AnyPathMaxSizeJsonFilter(-1, -1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}
	
	@Test
	public void test()  {
		int size = 17;
		AnyPathMaxSizeJsonFilter filter = new AnyPathMaxSizeJsonFilter(size, -1, new String[]{DEFAULT_ANY_PATH}, null) {
			@Override
			protected boolean mustConstrainMaxSize(int length) {
				return false;
			}
		};
		
		String process = filter.process("{\"key\":[123,456]}");
		System.out.println(process);
	}

}
