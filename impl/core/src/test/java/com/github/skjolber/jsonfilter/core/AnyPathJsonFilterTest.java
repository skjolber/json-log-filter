package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class AnyPathJsonFilterTest extends DefaultJsonFilterTest {

	public AnyPathJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new AnyPathJsonFilter(-1, new String[]{ANY_PASSTHROUGH_XPATH}, null)).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		AnyPathJsonFilter filter = new AnyPathJsonFilter(-1, new String[]{ANY_PASSTHROUGH_XPATH}, null);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}	

	@Test
	public void anonymizeAny() throws Exception {
		assertThat(new AnyPathJsonFilter(-1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
		assertThat(new AnyPathJsonFilter(-1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();
	}

	@Test
	public void pruneAny() throws Exception {
		assertThat(new AnyPathJsonFilter(-1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH).hasAnonymizeMetrics();
		assertThat(new AnyPathJsonFilter(-1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH).hasPruneMetrics();
	}

	@Test
	public void anonymizeAnyMaxPathMatches() throws Exception {
		assertThat(new AnyPathJsonFilter(1, new String[]{"//key1"}, null)).hasAnonymized("//key1").hasAnonymizeMetrics();
		assertThat(new AnyPathJsonFilter(2, new String[]{"//child1"}, null)).hasAnonymized("//child1").hasAnonymizeMetrics();
	}

	@Test
	public void pruneAnyMaxPathMatches() throws Exception {
		assertThat(new AnyPathJsonFilter(1, null, new String[]{"//key3"})).hasPruned("//key3").hasPruneMetrics();
	}

	@Test
	public void testConstructorWithMessages() throws Exception {
		AnyPathJsonFilter filter = new AnyPathJsonFilter(-1, new String[]{DEFAULT_ANY_PATH}, null,
			AbstractJsonFilter.FILTER_PRUNE_MESSAGE_JSON,
			AbstractJsonFilter.FILTER_ANONYMIZE_JSON,
			AbstractJsonFilter.FILTER_TRUNCATE_MESSAGE);
		assertThat(filter).isNotNull();
	}

	@Test
	public void testConstructorNullBothThrows() throws Exception {
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new AnyPathJsonFilter(-1, null, null);
		});
	}

	@Test
	public void anonymizeNonStringValuesWithPathMatches() throws Exception {
		// Test rangesAnyPath with pathMatches: covers true/false/null/number value branches
		AnyPathJsonFilter filter = new AnyPathJsonFilter(5, new String[]{"//boolKey"}, null);

		// true value
		String jsonTrue = "{\"boolKey\":true,\"boolKey\":true,\"boolKey\":true}";
		assertNotNull(filter.process(jsonTrue.toCharArray(), 0, jsonTrue.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonTrue.getBytes(StandardCharsets.UTF_8)));

		// null value
		filter = new AnyPathJsonFilter(5, new String[]{"//nullKey"}, null);
		String jsonNull = "{\"nullKey\":null,\"other\":\"data\"}";
		assertNotNull(filter.process(jsonNull.toCharArray(), 0, jsonNull.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonNull.getBytes(StandardCharsets.UTF_8)));

		// false value
		filter = new AnyPathJsonFilter(5, new String[]{"//falseKey"}, null);
		String jsonFalse = "{\"falseKey\":false,\"other\":\"data\"}";
		assertNotNull(filter.process(jsonFalse.toCharArray(), 0, jsonFalse.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonFalse.getBytes(StandardCharsets.UTF_8)));

		// number value
		filter = new AnyPathJsonFilter(5, new String[]{"//numKey"}, null);
		String jsonNum = "{\"numKey\":12345,\"other\":\"data\"}";
		assertNotNull(filter.process(jsonNum.toCharArray(), 0, jsonNum.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonNum.getBytes(StandardCharsets.UTF_8)));

		// object value
		filter = new AnyPathJsonFilter(5, new String[]{"//objKey"}, null);
		String jsonObj = "{\"objKey\":{\"inner\":\"val\"},\"other\":\"data\"}";
		assertNotNull(filter.process(jsonObj.toCharArray(), 0, jsonObj.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonObj.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void pruneNonStringValuesWithPathMatches() throws Exception {
		// Prune with non-string values + pathMatches
		AnyPathJsonFilter filter = new AnyPathJsonFilter(3, null, new String[]{"//boolKey"});
		String json = "{\"boolKey\":true,\"boolKey\":false,\"boolKey\":null,\"other\":\"data\"}";
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));
		assertNotNull(filter.process(json.getBytes(StandardCharsets.UTF_8)));

		// prune number
		filter = new AnyPathJsonFilter(3, null, new String[]{"//numKey"});
		String jsonNum = "{\"numKey\":42,\"other\":\"data\"}";
		assertNotNull(filter.process(jsonNum.toCharArray(), 0, jsonNum.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonNum.getBytes(StandardCharsets.UTF_8)));

		// prune object
		filter = new AnyPathJsonFilter(3, null, new String[]{"//objKey"});
		String jsonObj = "{\"objKey\":{\"a\":1},\"other\":\"data\"}";
		assertNotNull(filter.process(jsonObj.toCharArray(), 0, jsonObj.length(), new StringBuilder()));
		assertNotNull(filter.process(jsonObj.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testAnyPathPruneWithStarThrowsIllegalArgumentException() {
		// AbstractMultiPathJsonFilter line 53: throw when prune path is "//*" (any-match with star)
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new AnyPathJsonFilter(-1, null, new String[]{"//*"});
		});
	}

	@Test
	public void testAnyPathAnonWithStarThrowsIllegalArgumentException() {
		// AbstractMultiPathJsonFilter line 69: throw when anonymize path is "//*" (any-match with star)
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new AnyPathJsonFilter(-1, new String[]{"//*"}, null);
		});
	}

}
