package com.github.skjolber.jsonfilter.jackson;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonGenerator;

public class JacksonJsonFilterTest {

	@Test
	public void testDefaultMethods() throws IOException {
		
		JacksonJsonFilter mock = mock(JacksonJsonFilter.class);
	
		when(mock.process(any(char[].class), any(JsonGenerator.class))).thenCallRealMethod();
		when(mock.process(any(byte[].class), any(JsonGenerator.class))).thenCallRealMethod();
		when(mock.process(any(byte[].class), any(StringBuilder.class))).thenCallRealMethod();
		
		JsonGenerator generator = mock(JsonGenerator.class);
		
		mock.process(new char[] {}, generator);
		mock.process(new byte[] {}, generator);
		mock.process(new byte[] {}, new StringBuilder());
		
		verify(mock, times(1)).process(any(char[].class), any(JsonGenerator.class));
		verify(mock, times(1)).process(any(byte[].class), any(JsonGenerator.class));
		verify(mock, times(1)).process(any(byte[].class), any(StringBuilder.class));
	}
}
