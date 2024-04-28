package com.github.skjolber.jsonfilter.path;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.jackson.AbstractJacksonJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.path.matcher.AllJsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.path.matcher.DefaultJsonFilterPathMatcherFactory;
import com.github.skjolber.jsonfilter.path.matcher.JsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.path.matcher.PrefixJsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.path.properties.JsonFilterPathProperties;
import com.github.skjolber.jsonfilter.path.properties.JsonFilterProperties;
import com.github.skjolber.jsonfilter.path.properties.JsonFilterReplacementsProperties;
import com.github.skjolber.jsonfilter.path.properties.JsonFiltersProperties;

public class RequestResponseJsonFilterFactoryTest {

	private DefaultJsonFilterPathMatcherFactory pathMatcherFactory = new DefaultJsonFilterPathMatcherFactory();
	private RequestResponseJsonFilterFactory rrFactory = new RequestResponseJsonFilterFactory(pathMatcherFactory);

	@Test
	public void testDefaulCnfiguration() {
		JsonFiltersProperties properties = new JsonFiltersProperties();
		rrFactory.requestResponseJsonFilter(properties);
		
		assertTrue(pathMatcherFactory.createMatcher(null, null, null, null, null, 1024) instanceof AllJsonFilterPathMatcher);
		assertTrue(pathMatcherFactory.createMatcher("",  null, null, null, null, 1024) instanceof AllJsonFilterPathMatcher);
		
		assertTrue(pathMatcherFactory.createMatcher("/ABC", null, null, null, null, 1024) instanceof PrefixJsonFilterPathMatcher);
		
		JsonFilterPathProperties p = new JsonFilterPathProperties();
	
		JsonFilterProperties request = new JsonFilterProperties();
		request.setEnabled(false);
		request.setAnonymizes(Arrays.asList("/a"));
		request.setPrunes(Arrays.asList("/b"));
		request.setMaxPathMatches(1);
		request.setEnabled(true);
		
		p.setRequest(request);
	}
	
	@Test
	public void testEnabled() {
		JsonFiltersProperties properties = new JsonFiltersProperties();
		
		JsonFilterPathProperties p = new JsonFilterPathProperties();
	
		JsonFilterProperties request = new JsonFilterProperties();
		request.setEnabled(false);
		request.setAnonymizes(Arrays.asList("/a"));
		request.setPrunes(Arrays.asList("/b"));
		request.setMaxPathMatches(1);
		
		p.setRequest(request);
		
		properties.getPaths().add(p);

		RequestResponseJsonFilter requestResponseJsonFilter = rrFactory.requestResponseJsonFilter(properties);
		assertEquals(requestResponseJsonFilter.getRequests().length, 0);
		assertEquals(requestResponseJsonFilter.getResponses().length, 0);
	}	
	
	@Test 
	public void testAllForEmptyMatcher() {
		JsonFilter filter = new DefaultJsonFilter();
		JacksonMaxStringLengthJsonFilter validatingFilter = new JacksonMaxStringLengthJsonFilter(1024);
		JsonFilterPathMatcher matcher = pathMatcherFactory.createMatcher(null, validatingFilter, validatingFilter, filter, filter, 1024);
		assertTrue(matcher instanceof AllJsonFilterPathMatcher);
	}
	
	@Test
	public void createFilterWithValidation() {
		JsonFiltersProperties jsonFiltersProperties = new JsonFiltersProperties();
		
		JsonFilterProperties request = new JsonFilterProperties();
		request.setAnonymizes(Arrays.asList("/a"));
		request.setPrunes(Arrays.asList("/b"));
		request.setMaxPathMatches(1);
		request.setEnabled(true);
		request.setMaxSize(16 * 1024);
		
		JsonFilterPathProperties jsonFilterPathProperties = new JsonFilterPathProperties();
		jsonFilterPathProperties.setMatcher("/myPath");
		jsonFilterPathProperties.setRequest(request);
		
		jsonFiltersProperties.getPaths().add(jsonFilterPathProperties);
		
		JsonFilterReplacementsProperties replacements = new JsonFilterReplacementsProperties();
		
		replacements.setPrune("a");
		replacements.setAnonymize("b");
		replacements.setTruncate("c");

		jsonFiltersProperties.setReplacements(replacements);
		
		RequestResponseJsonFilter requestResponseJsonFilter = rrFactory.requestResponseJsonFilter(jsonFiltersProperties);
		
		JsonFilter filter = requestResponseJsonFilter.getRequestFilter("/myPath", true, 1024);
		
		assertTrue(filter instanceof JacksonJsonFilter);

		JsonFilter maxSizeFilter = requestResponseJsonFilter.getRequestFilter("/myPath", true, request.getMaxSize() + 1);
		assertTrue(maxSizeFilter instanceof JacksonJsonFilter);
		
		AbstractJsonFilter f = (AbstractJsonFilter) maxSizeFilter;
		assertTrue(f.getMaxSize() < Integer.MAX_VALUE);
	}

	@Test
	public void createFilterWithoutValidation() {
		JsonFiltersProperties jsonFiltersProperties = new JsonFiltersProperties();
		
		JsonFilterProperties request = new JsonFilterProperties();
		request.setAnonymizes(Arrays.asList("/a"));
		request.setPrunes(Arrays.asList("/b"));
		request.setMaxPathMatches(1);
		request.setMaxSize(1024);
		request.setEnabled(true);
		
		JsonFilterPathProperties jsonFilterPathProperties = new JsonFilterPathProperties();
		jsonFilterPathProperties.setMatcher("/myPath");
		jsonFilterPathProperties.setRequest(request);
		
		jsonFiltersProperties.getPaths().add(jsonFilterPathProperties);
		
		JsonFilterReplacementsProperties replacements = new JsonFilterReplacementsProperties();
		
		replacements.setPrune("a");
		replacements.setAnonymize("b");
		replacements.setTruncate("c");
		
		jsonFiltersProperties.setReplacements(replacements);
		
		RequestResponseJsonFilter requestResponseJsonFilter = rrFactory.requestResponseJsonFilter(jsonFiltersProperties);
		
		JsonFilter filter = requestResponseJsonFilter.getRequestFilter("/myPath", false, 1024);
		assertFalse(filter instanceof JacksonJsonFilter);
		
		JsonFilter maxSizeFilter = requestResponseJsonFilter.getRequestFilter("/myPath", false, request.getMaxSize() + 1);
		assertFalse(maxSizeFilter instanceof JacksonJsonFilter);
		
		AbstractJsonFilter f = (AbstractJsonFilter) maxSizeFilter;
		assertTrue(f.getMaxSize() < Integer.MAX_VALUE);
	}

	@Test
	public void testDisabled() {
		JsonFiltersProperties properties = new JsonFiltersProperties();
		properties.setEnabled(false);
		
		Assertions.assertThrows(IllegalStateException.class, () -> {
			rrFactory.requestResponseJsonFilter(properties);
		});

	}
	
}
