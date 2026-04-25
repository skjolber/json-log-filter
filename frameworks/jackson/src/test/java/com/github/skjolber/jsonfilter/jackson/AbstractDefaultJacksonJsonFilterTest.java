package com.github.skjolber.jsonfilter.jackson;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.spy;

import java.io.IOException;

import com.github.skjolber.jsonfilter.base.DefaultJsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public abstract class AbstractDefaultJacksonJsonFilterTest extends DefaultJsonFilterTest {

	public AbstractDefaultJacksonJsonFilterTest(boolean includePrettyPrinting) throws Exception {
		super(false, includePrettyPrinting, false);
	}
	
	public void testConvenienceMethods(JacksonJsonFilter successFilter, JacksonJsonFilter failureFilter, JacksonJsonFilter brokenFactory) throws IOException {
		// returns true
		byte[] jsonBytes = new byte[] {'{', '}'};
		char[] jsonChars = new char[] {'{', '}'};
		
		assertTrue(successFilter.process(jsonBytes, 0, 2, new StringBuilder()));
		assertTrue(successFilter.process(jsonChars, 0, 2, new StringBuilder()));

		assertTrue(successFilter.process(jsonBytes, new StringBuilder()));
		assertTrue(successFilter.process(jsonBytes, new StringBuilder(), new DefaultJsonFilterMetrics()));

		assertFalse(failureFilter.process(jsonBytes, 0, 2, new StringBuilder()));
		assertFalse(failureFilter.process(jsonChars, 0, 2, new StringBuilder()));
		
		assertFalse(brokenFactory.process(jsonBytes, 0, 2, new StringBuilder()));
		assertFalse(brokenFactory.process(jsonChars, 0, 2, new StringBuilder()));
	}

	private JacksonJsonFilter getFilter(JacksonJsonFilter filter) throws IOException {
		JacksonJsonFilter successFilter = spy(filter);
		
		doCallRealMethod().when(successFilter).process(any(byte[].class), any(int.class), any(int.class), any(StringBuilder.class));
		doCallRealMethod().when(successFilter).process(any(byte[].class), any(int.class), any(int.class), any(StringBuilder.class));
		return successFilter;
	}	
}
