package com.github.skjolber.jsonfilter.base;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;

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
		
		mock.process(new char[] {}, 0, 0, new StringBuilder());
		mock.process(new byte[] {}, 0, 0, new ByteArrayOutputStream());
		
		verify(charArrayRangesFilter, times(1)).filter(any(char[].class), any(int.class), any(int.class), any(StringBuilder.class));
		verify(byteArrayRangesFilter, times(1)).filter(any(byte[].class), any(int.class), any(int.class), any(ByteArrayOutputStream.class));
	}
}
