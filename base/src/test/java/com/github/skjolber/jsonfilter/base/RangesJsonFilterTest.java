package com.github.skjolber.jsonfilter.base;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

public class RangesJsonFilterTest {

	@Test
	public void testRanges() {
		RangesJsonFilter mock = mock(RangesJsonFilter.class);
		
		CharArrayRangesFilter charArrayRangesFilter = mock(CharArrayRangesFilter.class);
		ByteArrayRangesFilter byteArrayRangesFilter = mock(ByteArrayRangesFilter.class);
		
		when(mock.process(any(char[].class), any(int.class), any(int.class), any(StringBuilder.class))).thenCallRealMethod();
		when(mock.ranges(any(char[].class), any(int.class), any(int.class))).thenReturn(charArrayRangesFilter);
		
		when(mock.process(any(byte[].class), any(int.class), any(int.class), any(ByteArrayOutputStream.class))).thenCallRealMethod();
		when(mock.ranges(any(byte[].class), any(int.class), any(int.class))).thenReturn(byteArrayRangesFilter);
		
		assertTrue(mock.process(new char[] {}, 0, 0, new StringBuilder()));
		assertTrue(mock.process(new byte[] {}, 0, 0, new ByteArrayOutputStream()));
		
		verify(charArrayRangesFilter, times(1)).filter(any(char[].class), any(int.class), any(int.class), any(StringBuilder.class));
		verify(byteArrayRangesFilter, times(1)).filter(any(byte[].class), any(int.class), any(int.class), any(ByteArrayOutputStream.class));
	}
	
	@Test
	public void testNoFiltering() {
		RangesJsonFilter mock = mock(RangesJsonFilter.class);
		
		when(mock.process(any(char[].class), any(int.class), any(int.class), any(StringBuilder.class))).thenCallRealMethod();
		when(mock.ranges(any(char[].class), any(int.class), any(int.class))).thenReturn(null);
		
		when(mock.process(any(byte[].class), any(int.class), any(int.class), any(ByteArrayOutputStream.class))).thenCallRealMethod();
		when(mock.ranges(any(byte[].class), any(int.class), any(int.class))).thenReturn(null);
		
		assertFalse(mock.process(new char[] {}, 0, 0, new StringBuilder()));
		assertFalse(mock.process(new byte[] {}, 0, 0, new ByteArrayOutputStream()));
	}	
}
