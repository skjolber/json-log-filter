package com.github.skjolber.jsonfilter.jackson;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonGenerator;

import static org.mockito.Mockito.*;

import java.io.IOException;

public class JacksonJsonFilterTest {

	@Test
	public void testDefaultMethods() throws IOException {
		
		JacksonJsonFilter mock = mock(JacksonJsonFilter.class);
	
		when(mock.process(any(char[].class), any(JsonGenerator.class))).thenCallRealMethod();
		when(mock.process(any(byte[].class), any(JsonGenerator.class))).thenCallRealMethod();
		
		JsonGenerator generator = mock(JsonGenerator.class);
		
		mock.process(new char[] {}, generator);
		mock.process(new byte[] {}, generator);
		
		verify(mock, times(1)).process(any(char[].class), any(JsonGenerator.class));
		verify(mock, times(1)).process(any(byte[].class), any(JsonGenerator.class));
	}
}
