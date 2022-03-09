package com.github.skjolber.jsonfilter.base;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

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
		public boolean process(byte[] chars, int offset, int length, OutputStream output) {
			return false;
		}

	}

	@Test
	public void testConstructor() {
		new MyAbstractMultiPathJsonFilter(-1, -1, null, null, "pruneMessage", "anonymizeMessage", "truncateMessage");
		new MyAbstractMultiPathJsonFilter(127, 127, new String[] {"/abc", "//def"}, new String[] {"/abc", "//def"}, "pruneMessage", "anonymizeMessage", "truncateMessage");
		
		MyAbstractMultiPathJsonFilter filter = new MyAbstractMultiPathJsonFilter(127, 127, new String[] {"/abc/def/ghi", "/def", "/ghi", "/abc/yyy", "/abc/zzz"}, null, "pruneMessage", "anonymizeMessage", "truncateMessage");

		assertThat(filter.elementFilterStart).hasLength(4);
		assertThat(filter.elementFilterStart[0]).isEqualTo(0);
		assertThat(filter.elementFilterEnd[0]).isEqualTo(0);
		
		assertThat(filter.elementFilterStart[1]).isEqualTo(0);  
		assertThat(filter.elementFilterEnd[1]).isEqualTo(2); // /def and /ghi 
		
		assertThat(filter.elementFilterStart[2]).isEqualTo(2);
		assertThat(filter.elementFilterEnd[2]).isEqualTo(4); // /abc/yyy, /abc/zzz
		
		assertThat(filter.elementFilterStart[3]).isEqualTo(4);
		assertThat(filter.elementFilterEnd[3]).isEqualTo(5); // /abc/def/ghi  
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
	
	@Test
	public void testMatchElements() {
		AbstractMultiPathJsonFilter filter = new MyAbstractMultiPathJsonFilter(127, 127, new String[] {"/xzy", "/abc/def", "//def"}, null, "pruneMessage", "anonymizeMessage", "truncateMessage");
		
		String abc = "abc";
		String def = "def";
		
		// protected boolean matchElements(final byte[] chars, int start, int end, int level, final int[] elementMatches) {
		
		int start = 0;
		int end = 3;
		
		// strings
		assertFalse(filter.matchElements(abc, 1, new int[] {0, 0}));
		assertTrue(filter.matchElements(def, 2, new int[] {0, 1}));

		// incorrect level
		assertFalse(filter.matchElements(def, 2, new int[] {0, 0}));
		assertFalse(filter.matchElements(def, 2, new int[] {0, 2}));
		assertFalse(filter.matchElements(def, 2, new int[] {0, 3}));
		
		// chars
		assertNull(filter.matchElements(abc.toCharArray(), start, end, 1, new int[] {0, 0}));
		assertEquals(FilterType.ANON, filter.matchElements(def.toCharArray(), start, end, 2, new int[] {0, 1}));

		// incorrect level
		assertNull(filter.matchElements(def.toCharArray(), start, end, 2, new int[] {0, 0}));
		assertNull(filter.matchElements(def.toCharArray(), start, end, 2, new int[] {0, 2}));
		assertNull(filter.matchElements(def.toCharArray(), start, end, 2, new int[] {0, 3}));

		// prune over anonymize
		AbstractMultiPathJsonFilter conflicting = new MyAbstractMultiPathJsonFilter(127, 127, new String[] {"/bbb"}, new String[] {"/bbb"}, "pruneMessage", "anonymizeMessage", "truncateMessage");
		assertEquals(FilterType.PRUNE, conflicting.matchElements("bbb".toCharArray(), start, end, 1, new int[] {0, 0}));

		// bytes
		assertNull(filter.matchElements(abc.getBytes(StandardCharsets.UTF_8), start, end, 1, new int[] {0, 0}));
		assertEquals(FilterType.ANON, filter.matchElements(def.getBytes(StandardCharsets.UTF_8), start, end, 2, new int[] {0, 1}));
		
		// incorrect level
		assertNull(filter.matchElements(def.getBytes(StandardCharsets.UTF_8), start, end, 2, new int[] {0, 0}));
		assertNull(filter.matchElements(def.getBytes(StandardCharsets.UTF_8), start, end, 2, new int[] {0, 2}));
		assertNull(filter.matchElements(def.getBytes(StandardCharsets.UTF_8), start, end, 2, new int[] {0, 3}));
		
		// prune over anonymize
		assertEquals(FilterType.PRUNE, conflicting.matchElements("bbb".getBytes(StandardCharsets.UTF_8), start, end, 1, new int[] {0, 0}));
	}
	
	@Test
	public void testConstrain() {
		MyAbstractMultiPathJsonFilter filter = new MyAbstractMultiPathJsonFilter(127, 127, new String[] {"/a/b/c", "/d/e/f/h"}, null, "pruneMessage", "anonymizeMessage", "truncateMessage");

		int[] matches = new int[] {2, 2};
		filter.constrainMatchesCheckLevel(matches, 1);
		assertThat(matches).asList().containsExactly(1, 1);

		matches = new int[] {2, 2};
		filter.constrainMatchesCheckLevel(matches, 5);
		assertThat(matches).asList().containsExactly(2, 2);

		matches = new int[] {0, 1};
		filter.constrainMatchesCheckLevel(matches, 2);
		assertThat(matches).asList().containsExactly(0, 1);

		matches = new int[] {2, 2};
		filter.constrainMatches(matches, 1);
		assertThat(matches).asList().containsExactly(1, 1);
		
	}
	
}
