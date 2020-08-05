package com.github.skjolber.jsonfilter.base;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class AbstractMultiPathJsonFilterTest {

	private static class MySingleCharArrayAnyPathJsonFilter extends AbstractMultiPathJsonFilter {

		public MySingleCharArrayAnyPathJsonFilter(int maxStringLength, int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
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
		new MySingleCharArrayAnyPathJsonFilter(-1, -1, null, null, "pruneMessage", "anonymizeMessage", "truncateMessage");
		new MySingleCharArrayAnyPathJsonFilter(127, 127, new String[] {"/abc", "//def"}, new String[] {"/abc", "//def"}, "pruneMessage", "anonymizeMessage", "truncateMessage");
	}
}
