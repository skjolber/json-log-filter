package com.github.skjolber.jsonfilter.spring.logbook;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.zalando.logbook.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.DefaultJsonLogFilterBuilder;

public class JsonFilterHttpResponseTest {

	@Test
	public void testFilterJson() throws Exception {
		JsonFilter firstName = DefaultJsonLogFilterBuilder.createInstance().withAnonymize("/firstName").build();

		HttpResponse response = mock(HttpResponse.class);
		JsonFilterHttpResponse  jsonFilterHttpResponse = new  JsonFilterHttpResponse(response, firstName);

		Map<String, Object> document = new HashMap<>();
		document.put("firstName", "secret1");
		document.put("lastName", "secret2");
		ObjectMapper mapper = new ObjectMapper();
		
		when(response.getBodyAsString()).thenReturn(mapper.writeValueAsString(document));
		when(response.getBody()).thenReturn(mapper.writeValueAsBytes(document));
		
		Map<?, ?> readValue1 = mapper.readValue(jsonFilterHttpResponse.getBody(), Map.class);
		assertThat(readValue1.get("firstName")).isEqualTo("*****");
		
		Map<?, ?> readValue2 = mapper.readValue(jsonFilterHttpResponse.getBodyAsString(), Map.class);
		assertThat(readValue2.get("firstName")).isEqualTo("*****");
		
		// test forwarding
		when(response.getCharset()).thenReturn(StandardCharsets.UTF_8);
		assertThat(jsonFilterHttpResponse.getCharset()).isEqualTo(StandardCharsets.UTF_8);
		
		assertThat(jsonFilterHttpResponse.withBody()).isNull();
		assertThat(jsonFilterHttpResponse.withoutBody()).isNull();
	}
	
	@Test
	public void testFilterNoContent() throws Exception {
		HttpResponse response = mock(HttpResponse.class);
		JsonFilterHttpResponse  jsonFilterHttpResponse = new  JsonFilterHttpResponse(response, null);
		 
		when(response.getBodyAsString()).thenReturn(null);
		when(response.getBody()).thenReturn(null);

		assertThat(jsonFilterHttpResponse.getBody()).isNull();
		assertThat(jsonFilterHttpResponse.getBodyAsString()).isNull();

	}
	
	@Test
	public void testFilterInvalidContent() throws Exception {
		JsonFilter firstName = DefaultJsonLogFilterBuilder.createInstance().withAnonymize("/firstName").build();

		HttpResponse response = mock(HttpResponse.class);
		JsonFilterHttpResponse  jsonFilterHttpResponse = new  JsonFilterHttpResponse(response, firstName);
		
		String invalidJson = "{XXXX";
		when(response.getBodyAsString()).thenReturn(invalidJson);
		when(response.getBody()).thenReturn(invalidJson.getBytes(StandardCharsets.UTF_8));

		assertThat(jsonFilterHttpResponse.getBody()).isEqualTo(invalidJson.getBytes(StandardCharsets.UTF_8));
		assertThat(jsonFilterHttpResponse.getBodyAsString()).isEqualTo(invalidJson);

	}
}
