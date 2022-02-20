package com.github.skjolber.jsonfilter.spring.logbook;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.zalando.logbook.HttpRequest;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.DefaultJsonLogFilterBuilder;
import com.github.skjolber.jsonfilter.jackson.JacksonJsonLogFilterBuilder;
import com.github.skjolber.jsonfilter.path.RequestResponseJsonFilter;
import com.github.skjolber.jsonfilter.path.matcher.JsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.path.matcher.PrefixJsonFilterPathMatcher;

public class PathRequestFilterTest {

	@SuppressWarnings("unchecked")
	@Test
	public void testFilter() throws Exception {
		
		List<JsonFilterPathMatcher> requests = new ArrayList<>();
		List<JsonFilterPathMatcher> responses = new ArrayList<>();
		
		JsonFilter firstName = DefaultJsonLogFilterBuilder.createInstance().withAnonymize("/firstName").build();
		JsonFilter firstNameValidate = JacksonJsonLogFilterBuilder.createInstance().withAnonymize("/firstName").build();
		
		JsonFilter lastName = DefaultJsonLogFilterBuilder.createInstance().withAnonymize("/lastName").build();
		JsonFilter lastNameValidate = JacksonJsonLogFilterBuilder.createInstance().withAnonymize("/lastName").build();

		requests.add(new PrefixJsonFilterPathMatcher("/abc", firstName, firstNameValidate));
		responses.add(new PrefixJsonFilterPathMatcher("/def", lastName, lastNameValidate));
		
		RequestResponseJsonFilter requestResponseJsonFilter = new RequestResponseJsonFilter(requests, responses);

		assertNull(requestResponseJsonFilter.getRequestFilter("/aaa", false));
		assertNull(requestResponseJsonFilter.getResponseFilter("/aaa", false));

		assertNotNull(requestResponseJsonFilter.getRequestFilter("/abc", false));
		assertNull(requestResponseJsonFilter.getResponseFilter("/abc", false));

		assertNotNull(requestResponseJsonFilter.getResponseFilter("/def", false));
		assertNull(requestResponseJsonFilter.getRequestFilter("/def", false));
		
		// request
		PathRequestFilter filter = new PathRequestFilter(requestResponseJsonFilter, false, false, new JsonFactory());
		HttpRequest miss = mock(HttpRequest.class);
		when(miss.getContentType()).thenReturn("application/json");
		when(miss.getPath()).thenReturn("/xyz");
		assertThat(filter.filter(miss)).isNotInstanceOf(JsonHttpRequest.class);
		
		HttpRequest other = mock(HttpRequest.class);
		when(other.getContentType()).thenReturn("application/xml");
		assertThat(filter.filter(other)).isNotInstanceOf(JsonHttpRequest.class);

		HttpRequest match = mock(HttpRequest.class);
		when(match.getContentType()).thenReturn("application/json");
		when(match.getPath()).thenReturn("/abc");

		HttpRequest filtering = filter.filter(match);
		assertThat(filtering).isInstanceOf(JsonFilterHttpRequest.class);

		Map<String, Object> document = new HashMap<>();
		document.put("firstName", "secret1");
		document.put("lastName", "secret2");
		ObjectMapper mapper = new ObjectMapper();
		
		when(match.getBodyAsString()).thenReturn(mapper.writeValueAsString(document));
		when(match.getBody()).thenReturn(mapper.writeValueAsBytes(document));
		
		Map<String, Object> readValue1 = mapper.readValue(filtering.getBody(), Map.class);
		assertThat(readValue1.get("firstName")).isEqualTo("*****");
		
		Map<String, Object> readValue2 = mapper.readValue(filtering.getBodyAsString(), Map.class);
		assertThat(readValue2.get("firstName")).isEqualTo("*****");
	}
}
