package com.github.skjolber.jsonfilter.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
		new MyAbstractMultiPathJsonFilter(-1, -1, null, null, "pruneMessage", "anonymizeMessage", "truncateMessage");
		new MyAbstractMultiPathJsonFilter(127, 127, new String[] {"/abc", "//def"}, new String[] {"/abc", "//def"}, "pruneMessage", "anonymizeMessage", "truncateMessage");
		
		MyAbstractMultiPathJsonFilter filter = new MyAbstractMultiPathJsonFilter(127, 127, new String[] {"/abc/def/ghi", "/def", "/ghi", "/abc/yyy", "/abc/zzz"}, null, "pruneMessage", "anonymizeMessage", "truncateMessage");
	}

	@Test
	public void testMatchAny() {
		AbstractMultiPathJsonFilter filter = new MyAbstractMultiPathJsonFilter(127, 127, new String[] {"/abc/def", "//def"}, null, "pruneMessage", "anonymizeMessage", "truncateMessage");
		
		String def = "def";
		assertEquals(filter.matchAnyElements(def), FilterType.ANON);
		assertEquals(filter.matchAnyElements(def.toCharArray(), 0, 3), FilterType.ANON);
		assertEquals(filter.matchAnyElements(def.getBytes(StandardCharsets.UTF_8), 0, 3), FilterType.ANON);

		String de = "de";
		assertNull(filter.matchAnyElements(de));
		assertNull(filter.matchAnyElements(de.toCharArray(), 0, 2));
		assertNull(filter.matchAnyElements(de.getBytes(StandardCharsets.UTF_8), 0, 2));

		String defgh = "defgh";
		assertNull(filter.matchAnyElements(defgh));
		assertNull(filter.matchAnyElements(defgh.toCharArray(), 0, 5));
		assertNull(filter.matchAnyElements(defgh.getBytes(StandardCharsets.UTF_8), 0, 5));
		
		String fgh = "fgh";
		assertNull(filter.matchAnyElements(fgh));
		assertNull(filter.matchAnyElements(fgh.toCharArray(), 0, 3));
		assertNull(filter.matchAnyElements(fgh.getBytes(StandardCharsets.UTF_8), 0, 3));
	}
	
}
