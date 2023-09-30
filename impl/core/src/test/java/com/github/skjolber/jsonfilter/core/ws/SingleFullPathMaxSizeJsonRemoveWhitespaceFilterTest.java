package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair.MaxSizeJsonFilterFunction;

public class SingleFullPathMaxSizeJsonRemoveWhitespaceFilterTest extends DefaultJsonFilterTest {

	private static class MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter extends SingleFullPathMaxSizeRemoveWhitespaceJsonFilter {

		public MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
			super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
		}

		public MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
			super(maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
		}

		public MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(int maxSize, int maxPathMatches, String expression, FilterType type) {
			super(maxSize, maxPathMatches, expression, type);
		}

		@Override
		protected boolean mustConstrainMaxSize(int length) {
			return true;
		}
	};
	
	
	public SingleFullPathMaxSizeJsonRemoveWhitespaceFilterTest() throws Exception {
		super();
	}

	@Test
	@ResourceLock(value = "jackson")
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, "/CVE_Items/cve/CVE_data_meta", FilterType.ANON));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, "/CVE_Items/cve/CVE_data_meta", FilterType.PRUNE));
		
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1,"/CVE_Items/impact/baseMetricV2/severity", FilterType.ANON));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1,"/CVE_Items/impact/baseMetricV2/severity", FilterType.PRUNE));

		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1,"/CVE_Items/impact/baseMetricV2/impactScore", FilterType.ANON));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1,"/CVE_Items/impact/baseMetricV2/impactScore", FilterType.PRUNE));
		
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1,"/CVE_Items/impact/baseMetricV2/obtainAllPrivilege", FilterType.ANON));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1,"/CVE_Items/impact/baseMetricV2/obtainAllPrivilege", FilterType.PRUNE));
	}

	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, DEEP_PATH, FilterType.ANON));
	}
	
	@Test
	public void passthrough_success() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, PASSTHROUGH_XPATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathRemoveWhitespaceJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.ANON)).hasPassthrough();
	}
	
	@Test
	public void anonymize() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, DEFAULT_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathRemoveWhitespaceJsonFilter(-1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		
		maxSize = (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, DEEP_PATH1, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathRemoveWhitespaceJsonFilter(-1, DEEP_PATH1, FilterType.ANON)).hasAnonymized(DEEP_PATH1).hasAnonymizeMetrics();
	}
	
	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, 1, "/key1", FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathRemoveWhitespaceJsonFilter(1, "/key1", FilterType.ANON)).hasAnonymized("/key1").hasAnonymizeMetrics();
		
		maxSize = (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, 1, DEFAULT_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathRemoveWhitespaceJsonFilter(1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		
		maxSize = (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, 2, DEFAULT_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathRemoveWhitespaceJsonFilter(2, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
	}	

	@Test
	public void anonymizeWildcard() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, DEFAULT_WILDCARD_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathRemoveWhitespaceJsonFilter(-1, DEFAULT_WILDCARD_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_WILDCARD_PATH).hasAnonymizeMetrics();
	}
	
	@Test
	public void prune() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, DEFAULT_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathRemoveWhitespaceJsonFilter(-1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		
		maxSize = (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, DEEP_PATH3, FilterType.PRUNE);
		assertThat(new SingleFullPathRemoveWhitespaceJsonFilter(-1, DEEP_PATH3, FilterType.PRUNE)).hasPruned(DEEP_PATH3).hasPruneMetrics();
	}
	
	@Test
	public void pruneMaxPathMatches() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, 1, "/key3", FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathRemoveWhitespaceJsonFilter(1, "/key3", FilterType.PRUNE)).hasPruned("/key3");
		
		maxSize = (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, 1, DEFAULT_PATH, FilterType.PRUNE);
		assertThat(new SingleFullPathRemoveWhitespaceJsonFilter(1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		
		maxSize = (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, 2, DEFAULT_PATH, FilterType.PRUNE);
		assertThat(new SingleFullPathRemoveWhitespaceJsonFilter(2, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH).hasPruneMetrics();
	}	

	@Test
	public void pruneWildcard() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, DEFAULT_WILDCARD_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathRemoveWhitespaceJsonFilter(-1, DEFAULT_WILDCARD_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_WILDCARD_PATH).hasPruneMetrics();
	}
	
	@Test
	public void exception_returns_false() throws Exception {
		JsonFilter filter = new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(-1, -1, PASSTHROUGH_XPATH, FilterType.PRUNE);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertNull(filter.process(new byte[] {}, 1, 1));
	}
	
	@Test
	public void test2() throws Exception {
		//String path = "/home/skjolber/git/json-log-filter-github/support/test/target/classes/json/array/1d/objectValueTextArray.json";

		//String path ="/home/skjolber/git/json-log-filter-github/support/test/target/classes/json/text/single/object1xKeyDeep.json";
		
		//String path = "/home/skjolber/git/json-log-filter-github/support/test/target/classes/json/boolean/mixed/object2xArrayKeyBefore2.json";
		
		//String path = "/home/skjolber/git/json-log-filter-github/support/test/target/classes/json/array/1d/objectValueTextArray.json";

		String path = "/home/thomas/git/json-log-filter-github/impl/core/src/test/resources/test.json";
		//String path = "/home/skjolber/git/json-log-filter-github/support/test/target/classes/json/array/1d/objectValueNumberArray.json";
		
		byte[] byteArray = IOUtils.toByteArray(new FileInputStream(new File(path)));
		System.out.println(new String(byteArray));
		
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		bout.write(byteArray);
		
		/*
		int size = 39;
		for(int i = byteArray.length; i < size; i++) {
			bout.write(' ');
		}
		*/
		
		byte[] byteArrayWithWhitespace = bout.toByteArray();
		
		//String other = "{\"key\":[\"aaaaa\",\"bbbbbb\",\"cccccccc\"]}";
		
		for(int i = byteArrayWithWhitespace.length - 2; i < byteArrayWithWhitespace.length; i++) {
			//JsonFilter filter = new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(byteArray.length, -1, "/*", FilterType.PRUNE);
			JsonFilter filter = new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(i, -1, "/*", FilterType.ANON);
			
			//System.out.println("Input: " + new String(byteArrayWithWhitespace));
			//String processChars = filter.process(other);
			
			String processChars = filter.process(new String(byteArrayWithWhitespace, StandardCharsets.UTF_8));
			//byte[] processBytes = filter.process(byteArrayWithWhitespace);
			
			System.out.println(new String(processChars) + " " + i);
			//System.out.println(new String(processBytes));
		}
	}
	
	@Test
	public void test() {
		String string = "{\n"
				+ "  \"f0\" : {\n"
				+ "    \"f1\" : {\n"
				+ "      \"deep\" : \"value\"\n"
				+ "    }\n"
				+ "  }\n"
				+ "}";
		
		int size = 25;
		
		SingleFullPathMaxSizeRemoveWhitespaceJsonFilter filter = new MustContrainSingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, DEEP_PATH, FilterType.ANON);
		
		System.out.println("Original:");
		System.out.println(string);
		System.out.println("Filtered:");

		String filtered = filter.process(string);
		System.out.println(filtered);
		System.out.println(filtered.length());
		
		byte[] filteredBytes = filter.process(string.getBytes());
		System.out.println(new String(filteredBytes));
		System.out.println(filteredBytes.length);

	}
	
	

}