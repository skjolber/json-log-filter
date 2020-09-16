package com.github.skjolber.jsonfilter.base;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class AbstractMultiCharArrayPathFilterTest {

	public static class MyAbstractMultiCharArrayPathFilter extends AbstractMultiCharArrayPathFilter {

		public MyAbstractMultiCharArrayPathFilter(int maxStringLength, int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
			super(maxStringLength, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
		}

		@Override
		public boolean process(char[] chars, int offset, int length, StringBuilder output) {
			return false;
		}

		@Override
		public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
			return false;
		}
		
	}
	
	@Test
	public void testConstructor() {
		new MyAbstractMultiCharArrayPathFilter(-1, -1, null, null, "pruneMessage", "anonymizeMessage", "truncateMessage");
		new MyAbstractMultiCharArrayPathFilter(127, 127, new String[] {"/abc", "//def"}, new String[] {"/abc", "//def"}, "pruneMessage", "anonymizeMessage", "truncateMessage");
	}
	
	@Test
	public void testMatchAny() {
		AbstractMultiCharArrayPathFilter filter = new MyAbstractMultiCharArrayPathFilter(127, 127, new String[] {"/abc/def", "//def"}, null, "pruneMessage", "anonymizeMessage", "truncateMessage");
		
		String def = "def";
		assertEquals(filter.matchAnyElements(def.toCharArray(), 0, 3), FilterType.ANON);
		assertEquals(filter.matchAnyElements(def.getBytes(StandardCharsets.UTF_8), 0, 3), FilterType.ANON);

		String de = "de";
		assertNull(filter.matchAnyElements(de.toCharArray(), 0, 2));
		assertNull(filter.matchAnyElements(de.getBytes(StandardCharsets.UTF_8), 0, 2));

		String defgh = "defgh";
		assertNull(filter.matchAnyElements(defgh.toCharArray(), 0, 5));
		assertNull(filter.matchAnyElements(defgh.getBytes(StandardCharsets.UTF_8), 0, 5));
		
		String fgh = "fgh";
		assertNull(filter.matchAnyElements(fgh.toCharArray(), 0, 3));
		assertNull(filter.matchAnyElements(fgh.getBytes(StandardCharsets.UTF_8), 0, 3));
		
	}
	
	@Test
	public void testMatchElements() {
		AbstractMultiCharArrayPathFilter filter = new MyAbstractMultiCharArrayPathFilter(127, 127, new String[] {"/xzy", "/abc/def", "//def"}, null, "pruneMessage", "anonymizeMessage", "truncateMessage");
		
		String abc = "abc";
		String def = "def";
		
		// protected boolean matchElements(final byte[] chars, int start, int end, int level, final int[] elementMatches) {
		
		int start = 0;
		int end = 3;
		
		assertFalse(filter.matchElements(abc.toCharArray(), start, end, 1, new int[] {0, 0}));
		assertTrue(filter.matchElements(def.toCharArray(), start, end, 2, new int[] {0, 1}));

		// incorrect level
		assertFalse(filter.matchElements(def.toCharArray(), start, end, 2, new int[] {0, 0}));
		assertFalse(filter.matchElements(def.toCharArray(), start, end, 2, new int[] {0, 2}));
		assertFalse(filter.matchElements(def.toCharArray(), start, end, 2, new int[] {0, 3}));

		
		assertFalse(filter.matchElements(abc.getBytes(StandardCharsets.UTF_8), start, end, 1, new int[] {0, 0}));
		assertTrue(filter.matchElements(def.getBytes(StandardCharsets.UTF_8), start, end, 2, new int[] {0, 1}));
		
		// incorrect level
		assertFalse(filter.matchElements(def.getBytes(StandardCharsets.UTF_8), start, end, 2, new int[] {0, 0}));
		assertFalse(filter.matchElements(def.getBytes(StandardCharsets.UTF_8), start, end, 2, new int[] {0, 2}));
		assertFalse(filter.matchElements(def.getBytes(StandardCharsets.UTF_8), start, end, 2, new int[] {0, 3}));
	}		
	
}
