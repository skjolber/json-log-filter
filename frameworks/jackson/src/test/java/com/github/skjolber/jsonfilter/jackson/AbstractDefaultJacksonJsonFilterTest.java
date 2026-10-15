package com.github.skjolber.jsonfilter.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public abstract class AbstractDefaultJacksonJsonFilterTest extends DefaultJsonFilterTest {

	public AbstractDefaultJacksonJsonFilterTest(boolean includePrettyPrinting) throws Exception {
		super(false, includePrettyPrinting, false);
	}

	// the first root value is short, so that max size filters also read the start of the second root value
	protected final String multipleRootValues = readResource("/rootValues/multiple/objects.json");

	public void assertMultipleRootValuesRejected(JacksonJsonFilter filter) {
		// rejected, leaving the output as it was
		String prefix = "PREFIX|";

		StringBuilder fromChars = new StringBuilder(prefix);
		assertFalse(filter.process(multipleRootValues.toCharArray(), 0, multipleRootValues.length(), fromChars));
		assertEquals(prefix, fromChars.toString());

		byte[] bytes = multipleRootValues.getBytes(StandardCharsets.UTF_8);

		ResizableByteArrayOutputStream fromBytes = new ResizableByteArrayOutputStream(128);
		fromBytes.write(prefix.getBytes(StandardCharsets.UTF_8));
		assertFalse(filter.process(bytes, 0, bytes.length, fromBytes));
		assertEquals(prefix, fromBytes.toString(StandardCharsets.UTF_8));

		StringBuilder fromBytesToChars = new StringBuilder(prefix);
		assertFalse(filter.process(bytes, 0, bytes.length, fromBytesToChars));
		assertEquals(prefix, fromBytesToChars.toString());
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
