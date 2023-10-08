package com.github.skjolber.jsonfilter.path;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.jackson.DefaultJacksonJsonFilter;
import com.github.skjolber.jsonfilter.path.matcher.JsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.path.matcher.PrefixJsonFilterPathMatcher;

public class RequestResponseJsonFilterTest {
	
	@Test 
	public void testEmpty() {
		List<JsonFilterPathMatcher> requests = new ArrayList<>();
		List<JsonFilterPathMatcher> responses = new ArrayList<>();
		RequestResponseJsonFilter requestResponseJsonFilter = new RequestResponseJsonFilter(requests, responses);
		
		assertNull(requestResponseJsonFilter.getRequestFilter("/abc", false, 1024));
		assertNull(requestResponseJsonFilter.getResponseFilter("/abc", false, 1024));
	}

	@Test 
	public void testMatch() {
		List<JsonFilterPathMatcher> requests = new ArrayList<>();
		requests.add(new PrefixJsonFilterPathMatcher("/abc", new DefaultJacksonJsonFilter(), new DefaultJacksonJsonFilter(), new DefaultJsonFilter(), new DefaultJsonFilter(), 1024));

		List<JsonFilterPathMatcher> responses = new ArrayList<>();
		responses.add(new PrefixJsonFilterPathMatcher("/abc", new DefaultJacksonJsonFilter(), new DefaultJacksonJsonFilter(), new DefaultJsonFilter(), new DefaultJsonFilter(), 1024));

		RequestResponseJsonFilter requestResponseJsonFilter = new RequestResponseJsonFilter(requests, responses);
		
		assertNotNull(requestResponseJsonFilter.getRequestFilter("/abc", false, 1024));
		assertNotNull(requestResponseJsonFilter.getResponseFilter("/abc", false, 1024));
		
		assertNull(requestResponseJsonFilter.getRequestFilter("/def", false, 1024));
		assertNull(requestResponseJsonFilter.getResponseFilter("/def", false, 1024));
		
	}

}