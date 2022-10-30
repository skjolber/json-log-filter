package com.github.skjolber.jsonfilter.base;

import static com.github.skjolber.jsonfilter.base.AbstractJsonFilter.FILTER_ANONYMIZE_JSON;
import static com.github.skjolber.jsonfilter.base.AbstractJsonFilter.FILTER_PRUNE_MESSAGE_JSON;
import static com.github.skjolber.jsonfilter.base.AbstractJsonFilter.FILTER_TRUNCATE_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class SkipSubtreeJsonFilterTest  extends DefaultJsonFilterTest {

	public SkipSubtreeJsonFilterTest() throws Exception {
		super(true, 1, true);
	}

	@Test
	public void passthrough_success() throws Exception {
		SkipSubtreeJsonFilter filter = new SkipSubtreeJsonFilter(-1, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
		assertThat(filter).hasPassthrough();
	}
	
	@Test
	public void testSkipChars() {
		String endCurlyBracket = "abcde}";
		int skipSubtree = SkipSubtreeJsonFilter.skipSubtree(endCurlyBracket.toCharArray(), 0);
		assertEquals(skipSubtree, endCurlyBracket.length() - 1);
		
		String endComma = "abcde,";
		skipSubtree = SkipSubtreeJsonFilter.skipSubtree(endComma.toCharArray(), 0);
		assertEquals(skipSubtree, endComma.length() - 1);
		
		String endBracket = "abcde]";
		skipSubtree = SkipSubtreeJsonFilter.skipSubtree(endBracket.toCharArray(), 0);
		assertEquals(skipSubtree, endBracket.length() - 1);
		
		String quoted = "\"abcde\"";
		skipSubtree = SkipSubtreeJsonFilter.skipSubtree(quoted.toCharArray(), 0);
		assertEquals(skipSubtree, quoted.length());
	}	


	@Test
	public void testSkipBytes() {
		String endCurlyBracket = "abcde}";
		int skipSubtree = SkipSubtreeJsonFilter.skipSubtree(endCurlyBracket.getBytes(), 0);
		assertEquals(skipSubtree, endCurlyBracket.length() - 1);
		
		String endComma = "abcde,";
		skipSubtree = SkipSubtreeJsonFilter.skipSubtree(endComma.getBytes(), 0);
		assertEquals(skipSubtree, endComma.length() - 1);
		
		String endBracket = "abcde]";
		skipSubtree = SkipSubtreeJsonFilter.skipSubtree(endBracket.getBytes(), 0);
		assertEquals(skipSubtree, endBracket.length() - 1);
		
		String quoted = "\"abcde\"";
		skipSubtree = SkipSubtreeJsonFilter.skipSubtree(quoted.getBytes(), 0);
		assertEquals(skipSubtree, quoted.length());		
	}

}
