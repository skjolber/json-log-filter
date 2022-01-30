package com.github.skjolber.jsonfilter.spring.logbook;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.zalando.logbook.HttpRequest;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.DefaultJsonLogFilterBuilder;

public class JsonFilterHttpRequestTest {

	private JsonFactory jsonFactory = new JsonFactory();
	
	@Test
	public void testFilterJson() throws Exception {
		JsonFilter firstName = DefaultJsonLogFilterBuilder.createInstance().withAnonymize("/firstName").build();

		HttpRequest request = mock(HttpRequest.class);
		JsonFilterHttpRequest http = new JsonFilterHttpRequest(request, firstName, false, false, jsonFactory);

		Map<String, Object> document = new HashMap<>();
		document.put("firstName", "secret1");
		document.put("lastName", "secret2");
		ObjectMapper mapper = new ObjectMapper();
		
		when(request.getBodyAsString()).thenReturn(mapper.writeValueAsString(document));
		when(request.getBody()).thenReturn(mapper.writeValueAsBytes(document));
		
		Map<?, ?> readValue1 = mapper.readValue(http.getBody(), Map.class);
		assertThat(readValue1.get("firstName")).isEqualTo("*****");
		
		Map<?, ?> readValue2 = mapper.readValue(http.getBodyAsString(), Map.class);
		assertThat(readValue2.get("firstName")).isEqualTo("*****");

		// test forwarding
		when(request.getCharset()).thenReturn(StandardCharsets.UTF_8);
		when(request.getScheme()).thenReturn("https");
		when(request.getQuery()).thenReturn("a=b");
		when(request.getHost()).thenReturn("localhost");
		when(request.getPort()).thenReturn(Optional.of(443));

		assertThat(http.getCharset()).isEqualTo(StandardCharsets.UTF_8);
		assertThat(http.getScheme()).isEqualTo("https");
		assertThat(http.getQuery()).isEqualTo("a=b");
		assertThat(http.getHost()).isEqualTo("localhost");
		assertThat(http.getPort().get()).isEqualTo(443);
		
		assertThat(http.withBody()).isNull();
		assertThat(http.withoutBody()).isNull();
	}
	
	@Test
	public void testFilterNoContent() throws Exception {
		JsonFilter firstName = DefaultJsonLogFilterBuilder.createInstance().withAnonymize("/firstName").build();

		HttpRequest request = mock(HttpRequest.class);
		JsonFilterHttpRequest http = new JsonFilterHttpRequest(request, firstName, false, false, jsonFactory);
		 
		when(request.getBodyAsString()).thenReturn(null);
		when(request.getBody()).thenReturn(null);

		assertThat(http.getBody()).isNull();
		assertThat(http.getBodyAsString()).isNull();

	}
	
	@Test
	public void testFilterInvalidContent() throws Exception {
		JsonFilter firstName = DefaultJsonLogFilterBuilder.createInstance().withAnonymize("/firstName").build();

		HttpRequest request = mock(HttpRequest.class);
		JsonFilterHttpRequest http = new JsonFilterHttpRequest(request, firstName, false, false, jsonFactory);
		
		String invalidJson = "{XXXX";
		when(request.getBodyAsString()).thenReturn(invalidJson);
		when(request.getBody()).thenReturn(invalidJson.getBytes(StandardCharsets.UTF_8));

		String invalidEscapedJson =  "\"{XXXX\"";

		assertThat(http.getBody()).isEqualTo(invalidEscapedJson.getBytes(StandardCharsets.UTF_8));
		assertThat(http.getBodyAsString()).isEqualTo(invalidEscapedJson);

	}
}
