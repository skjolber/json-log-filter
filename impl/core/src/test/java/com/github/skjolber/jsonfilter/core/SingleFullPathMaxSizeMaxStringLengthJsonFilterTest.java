package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.core.pp.Indent;
import com.github.skjolber.jsonfilter.core.pp.PrettyPrintingJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxStringLengthRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.MaxSizeJsonFilterAdapter;

public class SingleFullPathMaxSizeMaxStringLengthJsonFilterTest extends DefaultJsonFilterTest {

	public SingleFullPathMaxSizeMaxStringLengthJsonFilterTest() throws Exception {
		super();
	}

	@Test
	@ResourceLock(value = "jackson")
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(128, size, -1, "/CVE_Items/cve/CVE_data_meta", FilterType.ANON));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(128, size, -1, "/CVE_Items/cve/CVE_data_meta", FilterType.PRUNE));
		
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(128, size, -1,"/CVE_Items/impact/baseMetricV2/severity", FilterType.ANON));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(128, size, -1,"/CVE_Items/impact/baseMetricV2/severity", FilterType.PRUNE));

		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(128, size, -1,"/CVE_Items/impact/baseMetricV2/impactScore", FilterType.ANON));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(128, size, -1,"/CVE_Items/impact/baseMetricV2/impactScore", FilterType.PRUNE));
		
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(128, size, -1,"/CVE_Items/impact/baseMetricV2/obtainAllPrivilege", FilterType.ANON));
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(128, size, -1,"/CVE_Items/impact/baseMetricV2/obtainAllPrivilege", FilterType.PRUNE));
	}
	
	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(128, size, -1,DEEP_PATH, FilterType.ANON));
	}
	
	@Test
	public void passthrough_success() throws Exception {
		MaxSizeJsonFilterAdapter maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, PASSTHROUGH_XPATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, -1, PASSTHROUGH_XPATH, FilterType.ANON)).hasPassthrough();
	}

	@Test
	public void anonymize() throws Exception {
		MaxSizeJsonFilterAdapter maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, DEFAULT_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		
		maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, DEEP_PATH1, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEEP_PATH1, FilterType.ANON)).hasAnonymized(DEEP_PATH1).hasAnonymizeMetrics();
	}

	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		MaxSizeJsonFilterAdapter maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, "/key1", FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 1, "/key1", FilterType.ANON)).hasAnonymized("/key1").hasAnonymizeMetrics();
		
		maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, DEFAULT_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		
		maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 2, DEFAULT_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 2, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
	}
	
	@Test
	public void anonymizeWildcard() throws Exception {
		MaxSizeJsonFilterAdapter maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, DEFAULT_WILDCARD_PATH, FilterType.ANON);
		
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_WILDCARD_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_WILDCARD_PATH).hasAnonymizeMetrics();
	}
	
	@Test
	public void prune() throws Exception {
		MaxSizeJsonFilterAdapter maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, DEFAULT_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		
		maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, DEEP_PATH3, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEEP_PATH3, FilterType.PRUNE)).hasPruned(DEEP_PATH3).hasPruneMetrics();
	}

	@Test
	public void pruneMaxPathMatches() throws Exception {
		MaxSizeJsonFilterAdapter maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, "/key3", FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 1, "/key3", FilterType.PRUNE)).hasPruned("/key3").hasPruneMetrics();
		
		maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, DEFAULT_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		
		maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 2, DEFAULT_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 2, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH).hasPruneMetrics();
	}

	@Test
	public void pruneWildcard() throws Exception {
		MaxSizeJsonFilterAdapter maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, "/key3", FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 1, "/key3", FilterType.PRUNE)).hasPruned("/key3").hasPruneMetrics();
	}

	@Test
	public void maxStringLength() throws Exception {
		MaxSizeJsonFilterAdapter maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, -1, PASSTHROUGH_XPATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, PASSTHROUGH_XPATH, FilterType.PRUNE)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH).hasMaxStringLengthMetrics()
		;
	}

	@Test
	public void maxStringLengthMaxStringLength() throws Exception {
		MaxSizeJsonFilterAdapter maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, 1, "/key3", FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, 1, "/key3", FilterType.PRUNE)).hasPruned("/key3").hasPruneMetrics();
	}

	@Test
	public void exception_returns_false() throws Exception {
		JsonFilter filter = new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, -1, -1, PASSTHROUGH_XPATH, FilterType.PRUNE);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertNull(filter.process(new byte[] {}, 1, 1));
	}	

	@Test
	public void exception_incorrect_level() throws Exception {
		JsonFilter filter = new SingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, FULL.length - 4, 127, PASSTHROUGH_XPATH, FilterType.PRUNE);
		assertFalse(filter.process(INCORRECT_LEVEL, new StringBuilder()));
		assertNull(filter.process(INCORRECT_LEVEL.getBytes(StandardCharsets.UTF_8)));
	}
	
	@Test
	public void maxStringLengthAnonymize() throws Exception {
		MaxSizeJsonFilterAdapter maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, -1, DEFAULT_PATH, FilterType.ANON);

		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, DEFAULT_PATH, FilterType.ANON))
			.hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH).hasMaxSizeMetrics()
			.hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
	}

	@Test
	public void maxStringLengthPrune() throws Exception {
		MaxSizeJsonFilterAdapter maxSize = (size) -> new SingleFullPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, -1, DEFAULT_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, DEFAULT_PATH, FilterType.PRUNE))
			.hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH).hasMaxStringLengthMetrics()
			.hasPruned(DEFAULT_PATH).hasPruneMetrics();
	}

}
