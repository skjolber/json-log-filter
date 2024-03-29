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
	public void testEncoding() throws IOException {
	
		StringBuilder output = new StringBuilder();
		AbstractJsonFilter.quoteAsString("\"", output);
		assertThat(output.toString()).isEqualTo("\\\"");

		output.setLength(0);
		AbstractJsonFilter.quoteAsString(new String(new byte[] {0}), output);
		assertThat(output.toString()).isEqualTo("\\u0000");
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
}
