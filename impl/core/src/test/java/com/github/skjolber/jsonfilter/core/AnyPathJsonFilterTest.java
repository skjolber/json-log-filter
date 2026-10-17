package com.github.skjolber.jsonfilter.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;
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
		// A wildcard-any prune path ("//*") is not a valid filter expression and must throw an IllegalArgumentException.
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new AnyPathJsonFilter(-1, null, new String[]{"//*"});
		});
	}

	@Test
	public void testAnyPathAnonWithStarThrowsIllegalArgumentException() {
		// A wildcard-any anonymize path ("//*") is not a valid filter expression and must throw an IllegalArgumentException.
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new AnyPathJsonFilter(-1, new String[]{"//*"}, null);
		});
	}

	@Test
	public void testProcessStringReturnsInputIfUnchanged() {
		AnyPathJsonFilter filter = new AnyPathJsonFilter(-1, new String[] {"//secret"}, null);

		String unchanged = "{\"a\":\"value\",\"b\":{\"c\":1}}";
		assertSame(unchanged, filter.process(unchanged));

		String changed = "{\"a\":\"value\",\"b\":{\"secret\":1}}";
		String output = filter.process(changed);
		assertNotSame(changed, output);
		assertEquals(filter.process(changed.toCharArray()), output);
	}

	@Test
	public void testProcessWithMetricsLikeWithoutMetrics() {
		// the same output and metrics as the char[] path and the output stream, without a copy if unchanged
		AnyPathJsonFilter filter = new AnyPathJsonFilter(-1, new String[] {"//secret"}, new String[] {"//other"});

		for(String json : new String[] {"{\"a\":\"value\"}", "{\"secret\":\"value\",\"other\":[1,2]}"}) {
			DefaultJsonFilterMetrics expected = new DefaultJsonFilterMetrics();
			String expectedOutput = filter.process(json.toCharArray(), expected);

			DefaultJsonFilterMetrics actual = new DefaultJsonFilterMetrics();
			String output = filter.process(json, actual);
			assertEquals(expectedOutput, output);
			assertMetricsEquals(expected, actual);
			if(json.equals(output)) {
				assertSame(json, output);
			}

			byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
			DefaultJsonFilterMetrics expectedBytes = new DefaultJsonFilterMetrics();
			ResizableByteArrayOutputStream stream = new ResizableByteArrayOutputStream(16);
			assertTrue(filter.process(bytes, 0, bytes.length, stream, expectedBytes));

			DefaultJsonFilterMetrics actualBytes = new DefaultJsonFilterMetrics();
			assertArrayEquals(stream.toByteArray(), filter.process(bytes, actualBytes));
			assertMetricsEquals(expectedBytes, actualBytes);
		}
	}

	private static void assertMetricsEquals(DefaultJsonFilterMetrics expected, DefaultJsonFilterMetrics actual) {
		assertEquals(expected.getInputSize(), actual.getInputSize());
		assertEquals(expected.getOutputSize(), actual.getOutputSize());
		assertEquals(expected.getMaxStringLength(), actual.getMaxStringLength());
		assertEquals(expected.getMaxSize(), actual.getMaxSize());
		assertEquals(expected.getAnonymize(), actual.getAnonymize());
		assertEquals(expected.getPrune(), actual.getPrune());
	}
}
