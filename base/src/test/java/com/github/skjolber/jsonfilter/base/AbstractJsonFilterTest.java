package com.github.skjolber.jsonfilter.base;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

public class AbstractJsonFilterTest {

	@Test
	public void testConvenienceMethods() throws IOException {
		
		AbstractJsonFilter mock = mock(AbstractJsonFilter.class);

		when(mock.process(any(String.class), any(StringBuilder.class))).thenCallRealMethod();
		when(mock.process(any(char[].class))).thenCallRealMethod();
		when(mock.process(any(String.class))).thenCallRealMethod();
		when(mock.process(any(Reader.class), any(Integer.class), any(StringBuilder.class))).thenCallRealMethod();
		when(mock.process(any(Reader.class), any(StringBuilder.class))).thenCallRealMethod();

		
		mock.process("", new StringBuilder());
		mock.process(new char[] {});
		mock.process("");
		mock.process(new StringReader(""), 0, new StringBuilder());
		mock.process(new StringReader(""), new StringBuilder());
		
		verify(mock, times(5)).process(any(char[].class), any(Integer.class), any(Integer.class), any(StringBuilder.class));
	}	
}
