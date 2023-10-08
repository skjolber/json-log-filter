package com.github.skjolber.jsonfilter.path;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
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

	private DefaultJsonFilterPathMatcherFactory defaultJsonFilterPathMatcherFactory = new DefaultJsonFilterPathMatcherFactory();
	private RequestResponseJsonFilterFactory c = new RequestResponseJsonFilterFactory(defaultJsonFilterPathMatcherFactory);

	@Test
	public void testAutoconfiguration() {
		JsonFiltersProperties properties = new JsonFiltersProperties();
		c.requestResponseJsonFilter(properties);
		
		assertTrue(defaultJsonFilterPathMatcherFactory.createMatcher(null, null, null, null, null, 1024) instanceof AllJsonFilterPathMatcher);
		assertTrue(defaultJsonFilterPathMatcherFactory.createMatcher("",  null, null, null, null, 1024) instanceof AllJsonFilterPathMatcher);
		
		assertTrue(defaultJsonFilterPathMatcherFactory.createMatcher("/ABC", null, null, null, null, 1024) instanceof PrefixJsonFilterPathMatcher);
		
		
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

		c.requestResponseJsonFilter(properties);

		RequestResponseJsonFilter requestResponseJsonFilter = c.requestResponseJsonFilter(properties);
		assertEquals(requestResponseJsonFilter.getRequests().length, 0);
		assertEquals(requestResponseJsonFilter.getResponses().length, 0);
	}	
	
	@Test 
	public void testAllForEmptyMatcher() {
		JsonFilter filter = new DefaultJsonFilter();
		JacksonMaxStringLengthJsonFilter validatingFilter = new JacksonMaxStringLengthJsonFilter(1024);
		JsonFilterPathMatcher matcher = defaultJsonFilterPathMatcherFactory.createMatcher(null, validatingFilter, validatingFilter, filter, filter, 1024);
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
		
		JsonFilterPathProperties jsonFilterPathProperties = new JsonFilterPathProperties();
		jsonFilterPathProperties.setMatcher("/myPath");
		jsonFilterPathProperties.setRequest(request);
		
		jsonFiltersProperties.getPaths().add(jsonFilterPathProperties);
		
		JsonFilterReplacementsProperties replacements = new JsonFilterReplacementsProperties();
		
		assertFalse(replacements.hasAnonymize());
		assertFalse(replacements.hasPrune());
		assertFalse(replacements.hasTruncate());

		replacements.setPrune("a");
		replacements.setAnonymize("b");
		replacements.setTruncate("c");

		assertTrue(replacements.hasAnonymize());
		assertTrue(replacements.hasPrune());
		assertTrue(replacements.hasTruncate());
		
		jsonFiltersProperties.setReplacements(replacements);
		
		RequestResponseJsonFilter requestResponseJsonFilter = c.requestResponseJsonFilter(jsonFiltersProperties);
		
		JsonFilter filter = requestResponseJsonFilter.getRequestFilter("/myPath", true, 1024);
		
		assertTrue(filter instanceof JacksonJsonFilter);
	}

	@Test
	public void createFilterWithoutValidation() {
		JsonFiltersProperties jsonFiltersProperties = new JsonFiltersProperties();
		
		JsonFilterProperties request = new JsonFilterProperties();
		request.setAnonymizes(Arrays.asList("/a"));
		request.setPrunes(Arrays.asList("/b"));
		request.setMaxPathMatches(1);
		request.setEnabled(true);
		
		JsonFilterPathProperties jsonFilterPathProperties = new JsonFilterPathProperties();
		jsonFilterPathProperties.setMatcher("/myPath");
		jsonFilterPathProperties.setRequest(request);
		
		jsonFiltersProperties.getPaths().add(jsonFilterPathProperties);
		
		JsonFilterReplacementsProperties replacements = new JsonFilterReplacementsProperties();
		
		assertFalse(replacements.hasAnonymize());
		assertFalse(replacements.hasPrune());
		assertFalse(replacements.hasTruncate());

		replacements.setPrune("a");
		replacements.setAnonymize("b");
		replacements.setTruncate("c");

		assertTrue(replacements.hasAnonymize());
		assertTrue(replacements.hasPrune());
		assertTrue(replacements.hasTruncate());
		
		jsonFiltersProperties.setReplacements(replacements);
		
		RequestResponseJsonFilter requestResponseJsonFilter = c.requestResponseJsonFilter(jsonFiltersProperties);
		
		JsonFilter filter = requestResponseJsonFilter.getRequestFilter("/myPath", false, 1024);
		
		assertFalse(filter instanceof JacksonJsonFilter);
	}

	@Test
	public void testDisabled() {
		JsonFiltersProperties properties = new JsonFiltersProperties();
		properties.setEnabled(false);
		
		Assertions.assertThrows(IllegalStateException.class, () -> {
			c.requestResponseJsonFilter(properties);
		});

	}
	
}
