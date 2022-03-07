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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;
public class AbstractJsonFilterTest {

	private class MyAbstractJsonFilter extends AbstractJsonFilter {

		public MyAbstractJsonFilter(int maxStringLength, String pruneJson, String anonymizeJson, String truncateJsonString) {
			super(maxStringLength, pruneJson, anonymizeJson, truncateJsonString, -1);
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
		when(successFilter.process(any(byte[].class), any(Integer.class), any(Integer.class), any(ByteArrayOutputStream.class))).thenReturn(true);
		
		assertTrue(successFilter.process("{}", new StringBuilder()));
		assertNotNull(successFilter.process(new char[] {}));
		assertNotNull(successFilter.process("{}"));
		assertTrue(successFilter.process(new StringReader("{}"), 2, new StringBuilder()));
		assertTrue(successFilter.process(new StringReader("{}"), -1, new StringBuilder()));
		assertTrue(successFilter.process(new StringReader("{}"), new StringBuilder()));
		
		verify(successFilter, times(6)).process(any(char[].class), any(Integer.class), any(Integer.class), any(StringBuilder.class));

		assertNotNull(successFilter.process(new byte[] {'{', '}'}));
		assertTrue(successFilter.process(new byte[] {'{', '}'}, new ByteArrayOutputStream()));
		assertTrue(successFilter.process(new ByteArrayInputStream(new byte[]{'{', '}'}), 2, new ByteArrayOutputStream()));
		assertTrue(successFilter.process(new ByteArrayInputStream(new byte[]{'{', '}'}), -1, new ByteArrayOutputStream()));
		assertTrue(successFilter.process(new ByteArrayInputStream(new byte[]{'{', '}'}), new ByteArrayOutputStream()));
		
		verify(successFilter, times(5)).process(any(byte[].class), any(Integer.class), any(Integer.class), any(ByteArrayOutputStream.class));
		
		assertThrows(EOFException.class, () -> {
			successFilter.process(new ByteArrayInputStream(new byte[]{'{', '}'}), 123, new ByteArrayOutputStream());
		});
		
		assertThrows(EOFException.class, () -> {
			successFilter.process(new StringReader("{}"), 123, new StringBuilder());
		});		
		
		
		AbstractJsonFilter failFilter = getJsonFilterMock();

		// abstract methods
		when(failFilter.process(any(char[].class), any(Integer.class), any(Integer.class), any(StringBuilder.class))).thenReturn(false);
		when(failFilter.process(any(byte[].class), any(Integer.class), any(Integer.class), any(ByteArrayOutputStream.class))).thenReturn(false);

		assertFalse(failFilter.process("{}", new StringBuilder()));
		assertNull(failFilter.process(new char[] {}));
		assertNull(failFilter.process("{}"));
		assertFalse(failFilter.process(new StringReader("{}"), 2, new StringBuilder()));
		assertFalse(failFilter.process(new StringReader("{}"), -1, new StringBuilder()));
		assertFalse(failFilter.process(new StringReader("{}"), new StringBuilder()));
		
		verify(failFilter, times(6)).process(any(char[].class), any(Integer.class), any(Integer.class), any(StringBuilder.class));

		assertNull(failFilter.process(new byte[] {'{', '}'}));
		assertFalse(failFilter.process(new byte[] {'{', '}'}, new ByteArrayOutputStream()));
		assertFalse(failFilter.process(new ByteArrayInputStream(new byte[]{'{', '}'}), 2, new ByteArrayOutputStream()));
		assertFalse(failFilter.process(new ByteArrayInputStream(new byte[]{'{', '}'}), -1, new ByteArrayOutputStream()));
		assertFalse(failFilter.process(new ByteArrayInputStream(new byte[]{'{', '}'}), new ByteArrayOutputStream()));
		
		verify(failFilter, times(5)).process(any(byte[].class), any(Integer.class), any(Integer.class), any(ByteArrayOutputStream.class));		
	}

	private AbstractJsonFilter getJsonFilterMock() throws IOException {
		AbstractJsonFilter mock = mock(AbstractJsonFilter.class);

		when(mock.process(any(String.class), any(StringBuilder.class))).thenCallRealMethod();
		when(mock.process(any(char[].class))).thenCallRealMethod();
		when(mock.process(any(String.class))).thenCallRealMethod();
		when(mock.process(any(Reader.class), any(Integer.class), any(StringBuilder.class))).thenCallRealMethod();
		when(mock.process(any(Reader.class), any(StringBuilder.class))).thenCallRealMethod();

		when(mock.process(any(byte[].class))).thenCallRealMethod();
		when(mock.process(any(byte[].class), any(ByteArrayOutputStream.class))).thenCallRealMethod();
		when(mock.process(any(InputStream.class), any(Integer.class), any(ByteArrayOutputStream.class))).thenCallRealMethod();
		when(mock.process(any(InputStream.class), any(ByteArrayOutputStream.class))).thenCallRealMethod();
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
	public void testEncoding() throws IOException {
	
		StringBuilder output = new StringBuilder();
		AbstractJsonFilter.quoteAsString("\"", output);
		assertThat(output.toString()).isEqualTo("\\\"");

		output.setLength(0);
		AbstractJsonFilter.quoteAsString(new String(new byte[] {0}), output);
		assertThat(output.toString()).isEqualTo("\\u0000");
	}
	
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
	
}
