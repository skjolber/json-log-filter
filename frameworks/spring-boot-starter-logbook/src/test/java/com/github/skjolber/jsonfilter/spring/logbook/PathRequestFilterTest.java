package com.github.skjolber.jsonfilter.spring.logbook;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.DefaultJsonLogFilterBuilder;
import com.github.skjolber.jsonfilter.spring.RequestResponseJsonFilter;
import com.github.skjolber.jsonfilter.spring.matcher.JsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.spring.matcher.PrefixJsonFilterPathMatcher;

public class PathRequestFilterTest {

	@Test
	public void testFilter() {
		
		List<JsonFilterPathMatcher> requests = new ArrayList<>();
		List<JsonFilterPathMatcher> responses = new ArrayList<>();
		
		JsonFilter firstName = DefaultJsonLogFilterBuilder.createInstance().withAnonymize("/firstName").build();
		JsonFilter lastName = DefaultJsonLogFilterBuilder.createInstance().withAnonymize("/lastName").build();

		requests.add(new PrefixJsonFilterPathMatcher("/abc", firstName));
		responses.add(new PrefixJsonFilterPathMatcher("/def", lastName));
		
		RequestResponseJsonFilter requestResponseJsonFilter = new RequestResponseJsonFilter(requests, responses);

		assertNull(requestResponseJsonFilter.getRequestFilter("/aaa"));
		assertNull(requestResponseJsonFilter.getResponseFilter("/aaa"));

		assertNotNull(requestResponseJsonFilter.getRequestFilter("/abc"));
		assertNull(requestResponseJsonFilter.getResponseFilter("/abc"));

		assertNotNull(requestResponseJsonFilter.getResponseFilter("/def"));
		assertNull(requestResponseJsonFilter.getRequestFilter("/def"));
		
		
	}
}
