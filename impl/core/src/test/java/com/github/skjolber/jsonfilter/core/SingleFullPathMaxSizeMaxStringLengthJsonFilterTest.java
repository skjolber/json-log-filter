package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair.MaxSizeJsonFilterFunction;

public class SingleFullPathMaxSizeMaxStringLengthJsonFilterTest extends DefaultJsonFilterTest {

	private static class MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter extends SingleFullPathMaxSizeMaxStringLengthJsonFilter {

		public MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
			super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
		}

		public MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type) {
			super(maxStringLength, maxSize, maxPathMatches, expression, type);
		}

		@Override
		protected boolean mustConstrainMaxSize(int length) {
			return true;
		}
	};
	
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
		validateDeepStructure( (size) -> new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(128, size, -1,DEEP_PATH, FilterType.ANON));
	}
	
	@Test
	public void passthrough_success() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, PASSTHROUGH_XPATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, -1, PASSTHROUGH_XPATH, FilterType.ANON)).hasPassthrough();
	}

	@Test
	public void anonymize() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, DEFAULT_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		
		maxSize = (size) -> new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, DEEP_PATH1, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEEP_PATH1, FilterType.ANON)).hasAnonymized(DEEP_PATH1).hasAnonymizeMetrics();
	}

	@Test
	public void anonymizeMaxPathMatches() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, "/key1", FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 1, "/key1", FilterType.ANON)).hasAnonymized("/key1").hasAnonymizeMetrics();
		
		maxSize = (size) -> new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, DEFAULT_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
		
		maxSize = (size) -> new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 2, DEFAULT_PATH, FilterType.ANON);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 2, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
	}
	
	@Test
	public void anonymizeWildcard() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, DEFAULT_WILDCARD_PATH, FilterType.ANON);
		
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_WILDCARD_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_WILDCARD_PATH).hasAnonymizeMetrics();
	}
	
	@Test
	public void prune() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, DEFAULT_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		
		maxSize = (size) -> new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, -1, DEEP_PATH3, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, -1, DEEP_PATH3, FilterType.PRUNE)).hasPruned(DEEP_PATH3).hasPruneMetrics();
	}

	@Test
	public void pruneMaxPathMatches() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, "/key3", FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 1, "/key3", FilterType.PRUNE)).hasPruned("/key3").hasPruneMetrics();
		
		maxSize = (size) -> new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, DEFAULT_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH).hasPruneMetrics();
		
		maxSize = (size) -> new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 2, DEFAULT_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 2, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH).hasPruneMetrics();
	}

	@Test
	public void pruneWildcard() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, size, 1, "/key3", FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(-1, 1, "/key3", FilterType.PRUNE)).hasPruned("/key3").hasPruneMetrics();
	}

	@Test
	public void maxStringLength() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, -1, PASSTHROUGH_XPATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, PASSTHROUGH_XPATH, FilterType.PRUNE)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH).hasMaxStringLengthMetrics();
	}

	@Test
	public void maxStringLengthMaxStringLength() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, 1, "/key3", FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, 1, "/key3", FilterType.PRUNE)).hasPruned("/key3").hasPruneMetrics();
	}

	@Test
	public void exception_returns_false() throws Exception {
		JsonFilter filter = new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, -1, -1, PASSTHROUGH_XPATH, FilterType.PRUNE);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertNull(filter.process(new byte[] {}, 1, 1));
	}	

	@Test
	public void exception_incorrect_level() throws Exception {
		JsonFilter filter = new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(-1, FULL.length - 4, 127, PASSTHROUGH_XPATH, FilterType.PRUNE);
		assertFalse(filter.process(INCORRECT_LEVEL, new StringBuilder()));
		assertNull(filter.process(INCORRECT_LEVEL.getBytes(StandardCharsets.UTF_8)));
	}
	
	@Test
	public void maxStringLengthAnonymize() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, -1, DEFAULT_PATH, FilterType.ANON);

		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, DEFAULT_PATH, FilterType.ANON))
			.hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH).hasMaxSizeMetrics()
			.hasAnonymized(DEFAULT_PATH).hasAnonymizeMetrics();
	}

	@Test
	public void maxStringLengthPrune() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, -1, DEFAULT_PATH, FilterType.PRUNE);
		assertThatMaxSize(maxSize, new SingleFullPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1, DEFAULT_PATH, FilterType.PRUNE))
			.hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH).hasMaxStringLengthMetrics()
			.hasPruned(DEFAULT_PATH).hasPruneMetrics();
	}

	@Test
	public void test() {
		// String string = "{\"key1\":\"aa\",\"key2\":\"abcdefghijklmnopqrstuvwxyz0123456789\"}";
		String string = "{\"CVE_data_type\":\"CVE\",\"CVE_data_format\":\"MITRE\",\"CVE_data_version\":\"4.0\",\"CVE_data_numberOfCVEs\":\"7128\",\"CVE_data_timestamp\":\"2019-10-11T08:31Z\",\"CVE_Items\":[{\"cve\":{\"data_type\":\"CVE\",\"data_format\":\"MITRE\",\"data_version\":\"4.0\",\"CVE_data_meta\":{\"ID\":\"*****\",\"ASSIGNER\":\"*****\"},\"affects\":{\"vendor\":{\"vendor_data\":[{\"vendor_name\":\"microsoft\",\"product\":{\"product_data\":[{\"product_name\":\"office\",\"version\":{\"version_data\":[{\"version_value\":\"2000\",\"version_affected\":\"=\"},{\"version_value\":\"2003\",\"version_affected\":\"=\"},{\"version_value\":\"xp\",\"version_affected\":\"=\"}]}},{\"product_name\":\"publisher\",\"version\":{\"version_data\":[{\"version_value\":\"2000\",\"version_affected\":\"=\"},{\"version_value\":\"2002\",\"version_affected\":\"=\"},{\"version_value\":\"2003\",\"version_affected\":\"=\"}]}}]}}]}},\"problemtype\":{\"problemtype_data\":[{\"description\":[{\"lang\":\"en\",\"value\":\"CWE-119\"}]}]},\"references\":{\"reference_data\":[{\"url\":\"http://secunia.com/advisories/21863\",\"name\":\"21863\",\"refsource\":\"SECUNIA\",\"tags\":[\"Patch\",\"Vendor Advisory\"]},{\"url\":\"http://securityreason.com/securityalert/1548\",\"name\":\"1548\",\"refsource\":\"SREASON\",\"tags\":[]},{\"url\":\"http://securitytracker.com/id?1016825\",\"name\":\"1016825\",\"refsource\":\"SECTRACK\",\"tags\":[]},{\"url\":\"http://www.computerterrorism.com/research/ct12-09-2006-2.htm\",\"name\":\"http://www.computerterrorism.com/research/ct12-09-2006-2.htm\",\"refsource\":\"MISC\",\"tags\":[\"Exploit\",\"Patch\",\"Vendor Advisory\"]},{\"url\":\"http://www.kb.cert.org/vuls/id/406236\",\"name\":\"VU#406236\",\"refsource\":\"CERT-VN\",\"tags\":[\"US Government Resource\"]},{\"url\":\"http://www.securityfocus.com/archive/1/445824/100/0/threaded\",\"name\":\"20060912 Computer Terrorism (UK) :: Incident Response Centre - Microsoft Publisher Font Parsing Vulnerability\",\"refsource\":\"BUGTRAQ\",\"tags\":[]},{\"url\":\"http://www.securityfocus.com/archive/1/446630/100/100/threaded\",\"name\":\"SSRT061187\",\"refsource\":\"HP\",\"tags\":[]},{\"url\":\"http://www.securityfocus.com/bid/19951\",\"name\":\"19951\",\"refsource\":\"BID\",\"tags\":[\"Patch\"]},{\"url\":\"http://www.us-cert.gov/cas/techalerts/TA06-255A.html\",\"name\":\"TA06-255A\",\"refsource\":\"CERT\",\"tags\":[\"US Government Resource\"]},{\"url\":\"http://www.vupen.com/english/advisories/2006/3565\",\"name\":\"ADV-2006-3565\",\"refsource\":\"VUPEN\",\"tags\":[]},{\"url\":\"https://docs.microsoft.com/en-us/security-updates/securitybulletins/2006/ms06-054\",\"name\":\"MS06-054\",\"refsource\":\"MS\",\"tags\":[]},{\"url\":\"https://exchange.xforce.ibmcloud.com/vulnerabilities/28648\",\"name\":\"publisher-pub-code-execution(28648)\",\"refsource\":\"XF\",\"tags\":[]},{\"url\":\"https://oval.cisecurity.org/repository/search/definition/oval%3Aorg.mitre.oval%3Adef%3A590\",\"name\":\"oval:org.mitre.oval:def:590\",\"refsource\":\"OVAL\",\"tags\":[]}]},\"description\":{\"description_data\":[{\"lang\":\"en\",\"value\":\"Stack-based buffer overflow in Microsoft Publisher 2000 through 2003 allows user-assisted remote attackers to execute arbitrary code via a crafted PUB file, which causes an overflow when parsing fonts.\"}]}}}]}";
		
		System.out.println("Input size is ");
		
		int size = 2884;
		
		SingleFullPathMaxSizeMaxStringLengthJsonFilter filter = new MustContrainSingleFullPathMaxSizeMaxStringLengthJsonFilter(128, size, -1, "/CVE_Items/cve/CVE_data_meta", FilterType.ANON);
		
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
