package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
		assertThat(new AnyPathJsonFilter(5, new String[]{"//boolKey"}, null)).hasAnonymized("//boolKey").hasAnonymizeMetrics();
		assertThat(new AnyPathJsonFilter(5, new String[]{"//nullKey"}, null)).hasAnonymized("//nullKey").hasAnonymizeMetrics();
		assertThat(new AnyPathJsonFilter(5, new String[]{"//falseKey"}, null)).hasAnonymized("//falseKey").hasAnonymizeMetrics();
		assertThat(new AnyPathJsonFilter(5, new String[]{"//numKey"}, null)).hasAnonymized("//numKey").hasAnonymizeMetrics();
		assertThat(new AnyPathJsonFilter(5, new String[]{"//objKey"}, null)).hasAnonymized("//objKey").hasAnonymizeMetrics();
	}

	@Test
	public void pruneNonStringValuesWithPathMatches() throws Exception {
		assertThat(new AnyPathJsonFilter(3, null, new String[]{"//boolKey"})).hasPruned("//boolKey").hasPruneMetrics();
		assertThat(new AnyPathJsonFilter(3, null, new String[]{"//numKey"})).hasPruned("//numKey").hasPruneMetrics();
		assertThat(new AnyPathJsonFilter(3, null, new String[]{"//objKey"})).hasPruned("//objKey").hasPruneMetrics();
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
