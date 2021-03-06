package com.github.skjolber.jsonfilter.jackson;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public abstract class AbstractJacksonJsonFilterTest extends DefaultJsonFilterTest {

	public AbstractJacksonJsonFilterTest() throws Exception {
		super(false);
	}
	
	public void testConvenienceMethods(JacksonJsonFilter successFilter, JacksonJsonFilter failureFilter, JacksonJsonFilter brokenFactory) throws IOException {
		JsonFactory jsonFactory = new JsonFactory();
		
		// returns true
		JsonGenerator generator = jsonFactory.createGenerator(new ByteArrayOutputStream());
		JsonParser parser = jsonFactory.createParser(new byte[] {});
		
		byte[] jsonBytes = new byte[] {'{', '}'};
		char[] jsonChars = new char[] {'{', '}'};
		
		assertTrue(successFilter.process(new ByteArrayInputStream(jsonBytes), generator));
		assertTrue(successFilter.process(jsonBytes, 0, 2, generator));
		assertTrue(successFilter.process(parser, generator));
		assertTrue(successFilter.process(jsonBytes, 0, 2, new ByteArrayOutputStream()));
		
		assertTrue(successFilter.process(jsonBytes, 0, 2, new StringBuilder()));
		assertTrue(successFilter.process(jsonChars, 0, 2, new StringBuilder()));

		// throws exception
		assertThrows(RuntimeException.class, () -> {
			failureFilter.process(new ByteArrayInputStream(jsonBytes), generator);
		});
		assertFalse(failureFilter.process(jsonBytes, 0, 2, generator));
		assertThrows(RuntimeException.class, () -> {
			failureFilter.process(parser, generator);
		});
		
		assertFalse(failureFilter.process(jsonBytes, 0, 2, new ByteArrayOutputStream()));
		assertFalse(failureFilter.process(jsonBytes, 0, 2, new StringBuilder()));
		assertFalse(failureFilter.process(jsonChars, 0, 2, new StringBuilder()));
		
		// broken factory
		assertThrows(RuntimeException.class, () -> {
			brokenFactory.process(new ByteArrayInputStream(jsonBytes), generator);
		});
		assertFalse(brokenFactory.process(jsonBytes, 0, 2, generator));
		assertThrows(RuntimeException.class, () -> {
			brokenFactory.process(parser, generator);
		});
		
		assertFalse(brokenFactory.process(jsonBytes, 0, 2, new ByteArrayOutputStream()));
		assertFalse(brokenFactory.process(jsonBytes, 0, 2, new StringBuilder()));
		assertFalse(brokenFactory.process(jsonChars, 0, 2, new StringBuilder()));
	}

	private JacksonJsonFilter getFilter(JacksonJsonFilter filter) throws IOException {
		JacksonJsonFilter successFilter = spy(filter);
		
		doCallRealMethod().when(successFilter).process(any(InputStream.class), any(JsonGenerator.class));
		doCallRealMethod().when(successFilter).process(any(byte[].class), any(int.class), any(int.class), any(StringBuilder.class));
		doCallRealMethod().when(successFilter).process(any(JsonParser.class), any(JsonGenerator.class));
		doCallRealMethod().when(successFilter).process(any(byte[].class), any(int.class), any(int.class), any(ByteArrayOutputStream.class));
		
		doCallRealMethod().when(successFilter).process(any(byte[].class), any(int.class), any(int.class), any(StringBuilder.class));
		doCallRealMethod().when(successFilter).process(any(char[].class), any(int.class), any(int.class), any(JsonGenerator.class));
		return successFilter;
	}	
}
