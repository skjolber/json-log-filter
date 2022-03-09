package com.github.skjolber.jsonfilter.spring.autoconfigure.logbook;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Sink;

import com.fasterxml.jackson.core.JsonFactory;
import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.path.RequestResponseJsonFilter;

public class PathFilterSinkTest {

	@Test
	public void testJson() {
		assertTrue(PathFilterSink.isJson("application/json"));
		assertTrue(PathFilterSink.isJson("application/test+json"));
		assertTrue(PathFilterSink.isJson("application/json;charset=abc"));
		assertTrue(PathFilterSink.isJson("application/test+json;charset=abc"));
		assertFalse(PathFilterSink.isJson("application/xml"));
		assertFalse(PathFilterSink.isJson("text/xml"));
	}
	
	@Test
	public void testSink() throws IOException {
		RequestResponseJsonFilter requestResponseJsonFilter = mock(RequestResponseJsonFilter.class);
		Sink sink = mock(Sink.class);
		PathFilterSink pathFilterSink = new PathFilterSink(sink, requestResponseJsonFilter, false, false, false, false, new JsonFactory());
		
		JsonFilter jsonFilter = mock(JsonFilter.class);
		when(requestResponseJsonFilter.getResponseFilter("/def", false)).thenReturn(jsonFilter);

		HttpRequest matchRequest = mock(HttpRequest.class);
		when(matchRequest.getPath()).thenReturn("/def");

		HttpResponse matchResponse = mock(HttpResponse.class);
		when(matchResponse.getContentType()).thenReturn("application/json");
		
		pathFilterSink.write(null, matchRequest, matchResponse);
		verify(sink, times(1)).write(any(), any(HttpRequest.class), any(HttpResponse.class));

		HttpResponse otherResponse = mock(HttpResponse.class);
		when(otherResponse.getContentType()).thenReturn("application/xml");

		pathFilterSink.write(null, matchRequest, otherResponse);
		verify(sink, times(2)).write(any(), any(HttpRequest.class), any(HttpResponse.class));

		HttpRequest missRequest = mock(HttpRequest.class);
		when(missRequest.getContentType()).thenReturn("application/json");
		when(missRequest.getPath()).thenReturn("/yyy");

		pathFilterSink.write(null, missRequest, matchResponse);
		pathFilterSink.write(null, missRequest);
		verify(sink, times(3)).write(any(), any(HttpRequest.class), any(HttpResponse.class));
	}
	
}
