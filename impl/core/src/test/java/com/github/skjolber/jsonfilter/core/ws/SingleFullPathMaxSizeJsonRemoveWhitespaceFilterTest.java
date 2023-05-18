package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.MaxSizeJsonFilterAdapter;

public class SingleFullPathMaxSizeJsonRemoveWhitespaceFilterTest extends DefaultJsonFilterTest {

	public SingleFullPathMaxSizeJsonRemoveWhitespaceFilterTest() throws Exception {
		super();
	}

	@Test
	@ResourceLock(value = "jackson")
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, "/CVE_Items/cve/CVE_data_meta", FilterType.ANON));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, "/CVE_Items/cve/CVE_data_meta", FilterType.PRUNE));
		
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1,"/CVE_Items/impact/baseMetricV2/severity", FilterType.ANON));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1,"/CVE_Items/impact/baseMetricV2/severity", FilterType.PRUNE));

		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1,"/CVE_Items/impact/baseMetricV2/impactScore", FilterType.ANON));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1,"/CVE_Items/impact/baseMetricV2/impactScore", FilterType.PRUNE));
		
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1,"/CVE_Items/impact/baseMetricV2/obtainAllPrivilege", FilterType.ANON));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1,"/CVE_Items/impact/baseMetricV2/obtainAllPrivilege", FilterType.PRUNE));
	}

	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, DEEP_PATH, FilterType.ANON));
	}
	
	@Test
	public void passthrough_success() throws Exception {
		MaxSizeJsonFilterAdapter maxSize = (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, PASSTHROUGH_XPATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathRemoveWhitespaceJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.ANON)).hasPassthrough();
	}
	
	@Test
	public void anonymize() throws Exception {
		MaxSizeJsonFilterAdapter maxSize = (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, DEFAULT_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathRemoveWhitespaceJsonFilter(-1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		
		maxSize = (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, DEEP_PATH1, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathRemoveWhitespaceJsonFilter(-1, DEEP_PATH1, FilterType.ANON)).hasAnonymized(DEEP_PATH1).hasAnonymizeMetrics();
	}
	
	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		MaxSizeJsonFilterAdapter maxSize = (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, 1, "/key1", FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathRemoveWhitespaceJsonFilter(1, "/key1", FilterType.ANON)).hasAnonymized("/key1").hasAnonymizeMetrics();
		
		maxSize = (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, 1, DEFAULT_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathRemoveWhitespaceJsonFilter(1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		
		maxSize = (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, 2, DEFAULT_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathRemoveWhitespaceJsonFilter(2, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
	}	

	@Test
	public void anonymizeWildcard() throws Exception {
		MaxSizeJsonFilterAdapter maxSize = (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, DEFAULT_WILDCARD_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathRemoveWhitespaceJsonFilter(-1, DEFAULT_WILDCARD_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_WILDCARD_PATH).hasAnonymizeMetrics();
	}
	
	@Test
	public void prune() throws Exception {
		MaxSizeJsonFilterAdapter maxSize = (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, DEFAULT_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathRemoveWhitespaceJsonFilter(-1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		
		maxSize = (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, DEEP_PATH3, FilterType.PRUNE);
		assertThat(new SingleFullPathRemoveWhitespaceJsonFilter(-1, DEEP_PATH3, FilterType.PRUNE)).hasPruned(DEEP_PATH3).hasPruneMetrics();
	}
	
	@Test
	public void pruneMaxPathMatches() throws Exception {
		MaxSizeJsonFilterAdapter maxSize = (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, 1, "/key3", FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathRemoveWhitespaceJsonFilter(1, "/key3", FilterType.PRUNE)).hasPruned("/key3");
		
		maxSize = (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, 1, DEFAULT_PATH, FilterType.PRUNE);
		assertThat(new SingleFullPathRemoveWhitespaceJsonFilter(1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		
		maxSize = (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, 2, DEFAULT_PATH, FilterType.PRUNE);
		assertThat(new SingleFullPathRemoveWhitespaceJsonFilter(2, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH).hasPruneMetrics();
	}	

	@Test
	public void pruneWildcard() throws Exception {
		MaxSizeJsonFilterAdapter maxSize = (size) -> new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(size, -1, DEFAULT_WILDCARD_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathRemoveWhitespaceJsonFilter(-1, DEFAULT_WILDCARD_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_WILDCARD_PATH).hasPruneMetrics();
	}
	
	@Test
	public void exception_returns_false() throws Exception {
		JsonFilter filter = new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(-1, -1, PASSTHROUGH_XPATH, FilterType.PRUNE);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertNull(filter.process(new byte[] {}, 1, 1));
	}
	
	@Test
	public void test() throws Exception {
		//String path = "/home/skjolber/git/json-log-filter-github/support/test/target/classes/json/array/1d/objectValueTextArray.json";

		//String path ="/home/skjolber/git/json-log-filter-github/support/test/target/classes/json/text/single/object1xKeyDeep.json";
		
		String path = "/home/skjolber/git/json-log-filter-github/support/test/target/classes/json/boolean/mixed/object2xArrayKeyBefore2.json";
		
		byte[] byteArray = IOUtils.toByteArray(new FileInputStream(new File(path)));
		System.out.println(new String(byteArray));
		
		JsonFilter filter = new SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(128, -1, "/key", FilterType.ANON);
		
		String processChars = filter.process(new String(byteArray, StandardCharsets.UTF_8));
		//byte[] processBytes = filter.process(byteArray);
		
		System.out.println(new String(processChars));
		//System.out.println(new String(processBytes));
		
	}
	

}
