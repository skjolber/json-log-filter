package com.github.skjolber.jsonfilter.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class AbstractMultiPathJsonFilterTest {

	private static class MyAbstractMultiPathJsonFilter extends AbstractMultiPathJsonFilter {

		public MyAbstractMultiPathJsonFilter(int maxStringLength, int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
			super(maxStringLength, -1, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
		}

		@Override
		public boolean process(char[] chars, int offset, int length, StringBuilder output) {
			return false;
		}

		@Override
		public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output) {
			return false;
		}

		@Override
		public boolean process(char[] chars, int offset, int length, StringBuilder output,
				JsonFilterMetrics filterMetrics) {
			return false;
		}

		@Override
		public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output,
				JsonFilterMetrics filterMetrics) {
			return false;
		}

	}

	@Test
	public void testConstructor() {
		new MyAbstractMultiPathJsonFilter(-1, -1, new String[] {"/abc", "//def"}, null, "pruneMessage", "anonymizeMessage", "truncateMessage");
		new MyAbstractMultiPathJsonFilter(-1, -1, null, new String[] {"/abc", "//def"}, "pruneMessage", "anonymizeMessage", "truncateMessage");
		new MyAbstractMultiPathJsonFilter(127, 127, new String[] {"/abc", "//def"}, new String[] {"/abc", "//def"}, "pruneMessage", "anonymizeMessage", "truncateMessage");
		
		MyAbstractMultiPathJsonFilter filter = new MyAbstractMultiPathJsonFilter(127, 127, new String[] {"/abc/def/ghi", "/def", "/ghi", "/abc/yyy", "/abc/zzz"}, null, "pruneMessage", "anonymizeMessage", "truncateMessage");
	}

	@Test
	public void testFilterTypeGetType() {
		// Cover AbstractPathJsonFilter.FilterType.getType()
		int anonType = FilterType.ANON.getType();
		int pruneType = FilterType.PRUNE.getType();
		org.junit.jupiter.api.Assertions.assertNotEquals(anonType, pruneType);
	}

	@Test
	public void testConstructorWithDotNotation() {
		// Cover AbstractPathJsonFilter.parse() with '$' prefix
		new MyAbstractMultiPathJsonFilter(-1, -1, new String[] {"$.a.b.c"}, null, "p", "a", "t");
		new MyAbstractMultiPathJsonFilter(-1, -1, null, new String[] {"$.a.b.c"}, "p", "a", "t");
	}

	@Test
	public void testOnlyAbsolutePrunes() {
		// Only abs path prunes (no any-path filters) → anyPathFilters = null
		MyAbstractMultiPathJsonFilter filter = new MyAbstractMultiPathJsonFilter(-1, -1, null, new String[] {"/abc"}, "p", "a", "t");
		assertNull(filter.anyPathFilters);
	}

	@Test
	public void testOnlyAbsoluteAnonymizes() {
		// Only abs path anonymizes (no any-path filters) → anyPathFilters = null
		MyAbstractMultiPathJsonFilter filter = new MyAbstractMultiPathJsonFilter(-1, -1, new String[] {"/abc"}, null, "p", "a", "t");
		assertNull(filter.anyPathFilters);
	}

	@Test
	public void testAnyPathPruneWithStarThrowsIllegalArgumentException() {
		// AbstractMultiPathJsonFilter line 53: throw when prune path is "//*" (any-match with star)
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new MyAbstractMultiPathJsonFilter(-1, -1, null, new String[]{"//*"}, "p", "a", "t");
		});
	}

	@Test
	public void testAnyPathAnonWithStarThrowsIllegalArgumentException() {
		// AbstractMultiPathJsonFilter line 69: throw when anonymize path is "//*" (any-match with star)
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new MyAbstractMultiPathJsonFilter(-1, -1, new String[]{"//*"}, null, "p", "a", "t");
		});
	}

}
