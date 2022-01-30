package com.github.skjolber.jsonfilter.path;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.path.matcher.JsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.path.matcher.PrefixJsonFilterPathMatcher;

public class RequestResponseJsonFilterTest {
	
	@Test 
	public void testEmpty() {
		List<JsonFilterPathMatcher> requests = new ArrayList<>();
		List<JsonFilterPathMatcher> responses = new ArrayList<>();
		RequestResponseJsonFilter requestResponseJsonFilter = new RequestResponseJsonFilter(requests, responses);
		
		assertNull(requestResponseJsonFilter.getRequestFilter("/abc"));
		assertNull(requestResponseJsonFilter.getResponseFilter("/abc"));
	}

	@Test 
	public void testMatch() {
		List<JsonFilterPathMatcher> requests = new ArrayList<>();
		requests.add(new PrefixJsonFilterPathMatcher("/abc", new DefaultJsonFilter()));

		List<JsonFilterPathMatcher> responses = new ArrayList<>();
		responses.add(new PrefixJsonFilterPathMatcher("/abc", new DefaultJsonFilter()));

		RequestResponseJsonFilter requestResponseJsonFilter = new RequestResponseJsonFilter(requests, responses);
		
		assertNotNull(requestResponseJsonFilter.getRequestFilter("/abc"));
		assertNotNull(requestResponseJsonFilter.getResponseFilter("/abc"));
		
		assertNull(requestResponseJsonFilter.getRequestFilter("/def"));
		assertNull(requestResponseJsonFilter.getResponseFilter("/def"));
		
	}

}