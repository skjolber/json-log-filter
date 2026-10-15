package com.github.skjolber.jsonfilter.base;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
public class AbstractJsonFilterTest {

	private class MyAbstractJsonFilter extends AbstractJsonFilter {

		public MyAbstractJsonFilter(int maxStringLength, String pruneJson, String anonymizeJson, String truncateJsonString) {
			super(maxStringLength, -1, pruneJson, anonymizeJson, truncateJsonString);
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

	private static class FilterWithMaxSize extends AbstractJsonFilter {
		public FilterWithMaxSize(int maxSize) {
			super(0, maxSize, "p", "a", "t");
		}
		@Override
		public boolean process(char[] chars, int offset, int length, StringBuilder output) { return false; }
		@Override
		public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output) { return false; }
		@Override
		public boolean process(char[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics m) { return false; }
		@Override
		public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics m) { return false; }
		public boolean testMustConstrain(int length) {
			return mustConstrainMaxSize(length);
		}
	}
	
	@Test
	public void testConstructor() throws IOException {
		assertThrows(IllegalArgumentException.class, () -> {
			new MyAbstractJsonFilter(-2, "pruneJson", "anonymizeJson", "truncateJsonString");
		});
		assertThrows(IllegalArgumentException.class, () -> {
			new MyAbstractJsonFilter(Integer.MAX_VALUE, "pruneJson", "anonymizeJson", "truncateJsonString");
		});
		
		MyAbstractJsonFilter myAbstractJsonFilter = new MyAbstractJsonFilter(-1, "pruneJson", "anonymizeJson", "truncateJsonString");
		assertEquals("pruneJson", new String(myAbstractJsonFilter.getPruneJsonValue()));
		assertEquals("anonymizeJson", new String(myAbstractJsonFilter.getAnonymizeJsonValue()));
		assertEquals("truncateJsonString", new String(myAbstractJsonFilter.getTruncateStringValue()));
	}

	@Test
	public void testConvenienceMethods() throws IOException {
		AbstractJsonFilter successFilter = getJsonFilterMock();

		// abstract methods
		when(successFilter.process(any(char[].class), any(Integer.class), any(Integer.class), any(StringBuilder.class))).thenReturn(true);
		when(successFilter.process(any(byte[].class), any(Integer.class), any(Integer.class), any(ResizableByteArrayOutputStream.class))).thenReturn(true);
		
		assertTrue(successFilter.process("{}", new StringBuilder()));
		assertNotNull(successFilter.process(new char[] {}));
		assertNotNull(successFilter.process("{}"));
		
		verify(successFilter, times(3)).process(any(char[].class), any(Integer.class), any(Integer.class), any(StringBuilder.class));

		assertNotNull(successFilter.process(new byte[] {'{', '}'}));
		
		verify(successFilter, times(1)).process(any(byte[].class), any(Integer.class), any(Integer.class), any(ResizableByteArrayOutputStream.class));
		
		AbstractJsonFilter failFilter = getJsonFilterMock();

		// abstract methods
		when(failFilter.process(any(char[].class), any(Integer.class), any(Integer.class), any(StringBuilder.class))).thenReturn(false);
		when(failFilter.process(any(byte[].class), any(Integer.class), any(Integer.class), any(ResizableByteArrayOutputStream.class))).thenReturn(false);

		assertFalse(failFilter.process("{}", new StringBuilder()));
		assertNull(failFilter.process(new char[] {}));
		assertNull(failFilter.process("{}"));
		
		verify(failFilter, times(3)).process(any(char[].class), any(Integer.class), any(Integer.class), any(StringBuilder.class));

		assertNull(failFilter.process(new byte[] {'{', '}'}));
		
		verify(failFilter, times(1)).process(any(byte[].class), any(Integer.class), any(Integer.class), any(ResizableByteArrayOutputStream.class));		
	}

	private AbstractJsonFilter getJsonFilterMock() throws IOException {
		AbstractJsonFilter mock = mock(AbstractJsonFilter.class);

		when(mock.process(any(String.class), any(StringBuilder.class))).thenCallRealMethod();
		when(mock.process(any(char[].class))).thenCallRealMethod();
		when(mock.process(any(String.class))).thenCallRealMethod();

		when(mock.process(any(byte[].class), any(Integer.class), any(Integer.class))).thenCallRealMethod();

		when(mock.process(any(byte[].class))).thenCallRealMethod();
		return mock;
	}
	
	@Test
	public void testInvalidInputs() throws IOException {
		assertThrows(IllegalArgumentException.class, () -> {
			new MyAbstractJsonFilter(-2, "pruneJson", "anonymizeJson", "truncateJsonString");
		});
		
		assertThrows(IllegalArgumentException.class, () -> {
			new MyAbstractJsonFilter(Integer.MAX_VALUE, "pruneJson", "anonymizeJson", "truncateJsonString");
		});
	}

	@Test
	public void testEncodingString() throws IOException {
	
		StringBuilder output = new StringBuilder();
		AbstractJsonFilter.quoteAsString("\"", output);
		assertThat(output.toString()).isEqualTo("\\\"");

		output.setLength(0);
		AbstractJsonFilter.quoteAsString(new String(new byte[] {0}), output);
		assertThat(output.toString()).isEqualTo("\\u0000");
	}
	
	@Test
	public void testEncodingChars() throws IOException {
	
		StringBuilder output = new StringBuilder();
		char[] charArray = "\"".toCharArray();
		AbstractJsonFilter.quoteAsString(charArray, 0, charArray.length, output);
		assertThat(output.toString()).isEqualTo("\\\"");

		output.setLength(0);
		charArray = new String(new byte[] {0}).toCharArray();
		AbstractJsonFilter.quoteAsString(charArray, 0, charArray.length, output);
		assertThat(output.toString()).isEqualTo("\\u0000");
	}
	
	
	@Test
	public void testNumberSize() {
		int value = 1;
		for(int i = 1; i < 11; i++) {
			assertEquals(i, AbstractJsonFilter.lengthToDigits(value));
			
			value = value * 10;
		}
	}

	/*
	@Test
	public void testGetFilters() {
		
		MyAbstractJsonFilter filter = new MyAbstractJsonFilter(-1, "pruneJson", "anonymizeJson", "truncateJsonString");
		
		CharArrayRangesFilter charArrayRangesFilter = filter.getCharArrayRangesFilter(Integer.MAX_VALUE);
		assertSame(charArrayRangesFilter.anonymizeMessage, filter.getAnonymizeJsonValue());
		assertSame(charArrayRangesFilter.pruneMessage, filter.getPruneJsonValue());
		assertSame(charArrayRangesFilter.truncateMessage, filter.getTruncateStringValue());
		
		ByteArrayRangesFilter byteArrayRangesFilter = filter.getByteArrayRangesFilter(Integer.MAX_VALUE);
		assertEquals(new String(byteArrayRangesFilter.anonymizeMessage), new String(filter.getAnonymizeJsonValue()));
		assertEquals(new String(byteArrayRangesFilter.pruneMessage), new String(filter.getPruneJsonValue()));
		assertEquals(new String(byteArrayRangesFilter.truncateMessage), new String(filter.getTruncateStringValue()));
	}
*/

	@Test
	public void testGetMaxSizeDefault() {
		MyAbstractJsonFilter filter = new MyAbstractJsonFilter(-1, "p", "a", "t");
		assertThat(filter.getMaxSize()).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	public void testGetMaxStringLength() {
		MyAbstractJsonFilter filter = new MyAbstractJsonFilter(50, "p", "a", "t");
		assertThat(filter.getMaxStringLength()).isEqualTo(50);
	}

	@Test
	public void testGetMaxStringLengthDefault() {
		MyAbstractJsonFilter filter = new MyAbstractJsonFilter(-1, "p", "a", "t");
		assertThat(filter.getMaxStringLength()).isGreaterThan(0);
	}

	@Test
	public void testGetMaxSizeWithLimit() {
		AbstractJsonFilter withSize = new AbstractJsonFilter(0, 100, "p", "a", "t") {
			@Override
			public boolean process(char[] chars, int offset, int length, StringBuilder output) { return false; }
			@Override
			public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output) { return false; }
			@Override
			public boolean process(char[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics m) { return false; }
			@Override
			public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics m) { return false; }
		};
		assertThat(withSize.getMaxSize()).isEqualTo(100);
	}

	@Test
	public void testQuoteAsStringCharsWithTab() {
		StringBuilder output = new StringBuilder();
		char[] tabChar = new char[]{'\t'};
		AbstractJsonFilter.quoteAsString(tabChar, 0, 1, output);
		assertThat(output.toString()).isEqualTo("\\t");
	}

	@Test
	public void testConvenienceMethodsWithMetrics() throws Exception {
		AbstractJsonFilter mockFilter = getJsonFilterMockWithMetrics();
		JsonFilterMetrics metrics = mock(JsonFilterMetrics.class);

		when(mockFilter.process(any(char[].class), any(Integer.class), any(Integer.class), any(StringBuilder.class), any(JsonFilterMetrics.class))).thenReturn(true);
		when(mockFilter.process(any(byte[].class), any(Integer.class), any(Integer.class), any(ResizableByteArrayOutputStream.class), any(JsonFilterMetrics.class))).thenReturn(true);

		// process(String, StringBuilder, JsonFilterMetrics)
		assertThat(mockFilter.process("{}", new StringBuilder(), metrics)).isTrue();

		// process(String, JsonFilterMetrics) via process(char[], JsonFilterMetrics)
		assertNotNull(mockFilter.process("{}", metrics));

		// process(byte[], int, int, JsonFilterMetrics)
		assertNotNull(mockFilter.process(new byte[]{'{', '}'}, 0, 2, metrics));

		// process(byte[], JsonFilterMetrics) - 2-param version
		assertNotNull(mockFilter.process(new byte[]{'{', '}'}, metrics));
	}

	@Test
	public void testConvenienceMethodsWithMetricsReturnNull() throws Exception {
		AbstractJsonFilter mockFilter = getJsonFilterMockWithMetrics();
		JsonFilterMetrics metrics = mock(JsonFilterMetrics.class);

		// When underlying process returns false, the convenience methods should return null
		when(mockFilter.process(any(char[].class), any(Integer.class), any(Integer.class), any(StringBuilder.class), any(JsonFilterMetrics.class))).thenReturn(false);
		when(mockFilter.process(any(byte[].class), any(Integer.class), any(Integer.class), any(ResizableByteArrayOutputStream.class), any(JsonFilterMetrics.class))).thenReturn(false);

		// process(char[], JsonFilterMetrics) returns null when underlying returns false
		assertNull(mockFilter.process("{}".toCharArray(), metrics));

		// process(byte[], int, int, JsonFilterMetrics) returns null when underlying returns false
		assertNull(mockFilter.process(new byte[]{'{', '}'}, 0, 2, metrics));
	}


	private AbstractJsonFilter getJsonFilterMockWithMetrics() throws IOException {
		AbstractJsonFilter m = mock(AbstractJsonFilter.class);
		when(m.process(any(String.class), any(StringBuilder.class), any(JsonFilterMetrics.class))).thenCallRealMethod();
		when(m.process(any(String.class), any(JsonFilterMetrics.class))).thenCallRealMethod();
		when(m.process(any(char[].class), any(JsonFilterMetrics.class))).thenCallRealMethod();
		when(m.process(any(byte[].class), any(Integer.class), any(Integer.class), any(JsonFilterMetrics.class))).thenCallRealMethod();
		when(m.process(any(byte[].class), any(JsonFilterMetrics.class))).thenCallRealMethod();
		return m;
	}

	@Test
	public void testMustConstrainMaxSize() {
		FilterWithMaxSize filter = new FilterWithMaxSize(100);
		assertTrue(filter.testMustConstrain(101));  // length > maxSize → true
		assertFalse(filter.testMustConstrain(50));   // length <= maxSize → false
		assertFalse(filter.testMustConstrain(100));  // length == maxSize → false
	}

	@Test
	public void testQuoteAsStringCharsNoEscaping() {
		// chars that don't need escaping - covers the 'return' in inner loop
		StringBuilder output = new StringBuilder();
		char[] noEscapeChars = new char[]{'a', 'b', 'c'};
		AbstractJsonFilter.quoteAsString(noEscapeChars, 0, noEscapeChars.length, output);
		assertThat(output.toString()).isEqualTo("abc");

		// mixed: some chars need escaping, some don't
		output.setLength(0);
		char[] mixedChars = new char[]{'a', '"', 'b'};
		AbstractJsonFilter.quoteAsString(mixedChars, 0, mixedChars.length, output);
		assertThat(output.toString()).isEqualTo("a\\\"b");
	}
}
