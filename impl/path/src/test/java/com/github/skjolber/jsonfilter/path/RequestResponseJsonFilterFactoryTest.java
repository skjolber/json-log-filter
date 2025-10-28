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
import com.github.skjolber.jsonfilter.path.properties.ProcessingProperties;
import com.github.skjolber.jsonfilter.path.properties.WhitespaceStrategy;

public class RequestResponseJsonFilterFactoryTest {

	private DefaultJsonFilterPathMatcherFactory pathMatcherFactory = new DefaultJsonFilterPathMatcherFactory();
	private RequestResponseJsonFilterFactory rrFactory = new RequestResponseJsonFilterFactory(pathMatcherFactory);

	@Test
	public void testDefaulConfiguration() {
		JsonFiltersProperties properties = new JsonFiltersProperties();
		properties.setRequests(new ProcessingProperties(true, WhitespaceStrategy.ON_DEMAND));
		properties.setResponses(new ProcessingProperties(false, WhitespaceStrategy.NEVER));
		
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
		p.setResponse(request);
		
		JsonFilterPathProperties jsonFilterPathProperties = new JsonFilterPathProperties();
		jsonFilterPathProperties.setMatcher("/myPath");
		jsonFilterPathProperties.setRequest(request);
		
		properties.getPaths().add(jsonFilterPathProperties);
		properties.getRequests().setWhitespaceStrategy(WhitespaceStrategy.ALWAYS);
		assertTrue(properties.getRequests().hasWhitespaceStrategy());

		assertFalse(properties.getRequests().hasMaxSize());
		properties.getRequests().setMaxSize(123);
		assertTrue(properties.getRequests().hasMaxSize());
		assertEquals(123, properties.getRequests().getMaxSize());

		properties.getRequests().setValidate(true);
		assertTrue(properties.getRequests().isValidate());

	}
	
	@Test
	public void testDisabled1() {
		JsonFiltersProperties properties = new JsonFiltersProperties();
		
		JsonFilterPathProperties p = new JsonFilterPathProperties();
	
		JsonFilterProperties request = new JsonFilterProperties();
		request.setEnabled(false);
		request.setAnonymizes(Arrays.asList("/a"));
		request.setPrunes(Arrays.asList("/b"));
		request.setMaxPathMatches(1);
		request.setMaxStringLength(1024);
		
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
		jsonFilterPathProperties.setResponse(request);
		
		jsonFiltersProperties.getPaths().add(jsonFilterPathProperties);
		
		JsonFilterReplacementsProperties replacements = new JsonFilterReplacementsProperties();
		
		replacements.setPrune("a");
		replacements.setAnonymize("b");
		replacements.setTruncate("c");

		jsonFiltersProperties.setReplacements(replacements);
		
		RequestResponseJsonFilter requestResponseJsonFilter = rrFactory.requestResponseJsonFilter(jsonFiltersProperties);
		
		JsonFilter requestFilter = requestResponseJsonFilter.getRequestFilter("/myPath", true, 1024);
		
		assertTrue(requestFilter instanceof JacksonJsonFilter);

		JsonFilter maxSizeFilter = requestResponseJsonFilter.getRequestFilter("/myPath", true, request.getMaxSize() + 1);
		assertTrue(maxSizeFilter instanceof JacksonJsonFilter);
		
		AbstractJsonFilter f = (AbstractJsonFilter) maxSizeFilter;
		assertTrue(f.getMaxSize() < Integer.MAX_VALUE);
		
		JsonFilter responseFilter = requestResponseJsonFilter.getResponseFilter("/myPath", true, 1024);
		assertTrue(responseFilter instanceof JacksonJsonFilter);
	}

	@Test
	public void createFilterWithValidationMaxSize() {
		JsonFiltersProperties jsonFiltersProperties = new JsonFiltersProperties();
		
		JsonFilterProperties request = new JsonFilterProperties();
		request.setAnonymizes(Arrays.asList("/a"));
		request.setPrunes(Arrays.asList("/b"));
		request.setMaxPathMatches(1);
		request.setEnabled(true);
		
		JsonFilterPathProperties jsonFilterPathProperties = new JsonFilterPathProperties();
		jsonFilterPathProperties.setMatcher("/myPath");
		jsonFilterPathProperties.setRequest(request);
		jsonFilterPathProperties.setResponse(request);
		
		jsonFiltersProperties.getPaths().add(jsonFilterPathProperties);
		
		JsonFilterReplacementsProperties replacements = new JsonFilterReplacementsProperties();
		
		replacements.setPrune("a");
		replacements.setAnonymize("b");
		replacements.setTruncate("c");

		jsonFiltersProperties.setReplacements(replacements);
		
		RequestResponseJsonFilter requestResponseJsonFilter = rrFactory.requestResponseJsonFilter(jsonFiltersProperties);
		
		JsonFilter requestFilter = requestResponseJsonFilter.getRequestFilter("/myPath", true, 1024);
		
		assertTrue(requestFilter instanceof JacksonJsonFilter);

		JsonFilter maxSizeFilter = requestResponseJsonFilter.getRequestFilter("/myPath", true, request.getMaxSize() + 1);
		assertTrue(maxSizeFilter instanceof JacksonJsonFilter);
		
		AbstractJsonFilter f = (AbstractJsonFilter) maxSizeFilter;
		assertEquals(Integer.MAX_VALUE, f.getMaxSize());
		
		JsonFilter responseFilter = requestResponseJsonFilter.getResponseFilter("/myPath", true, 1024);
		assertTrue(responseFilter instanceof JacksonJsonFilter);
	}

	@Test
	public void createFilterWithoutValidationMaxSize() {
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
		
		jsonFiltersProperties.setPaths(Arrays.asList(jsonFilterPathProperties));
		
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
		
		assertFalse(filter.isRemovingWhitespace());
		assertTrue(maxSizeFilter.isRemovingWhitespace());

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
		assertEquals(f.getMaxSize(), Integer.MAX_VALUE);
		
		assertFalse(filter.isRemovingWhitespace());
		
		// TODO on-demand does not make sense if also the max size is set
		assertFalse(maxSizeFilter.isRemovingWhitespace());

	}


	@Test
	public void createFilterWithoutValidationAlwaysRemoveWhitespace() {
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
		jsonFiltersProperties.getRequests().setWhitespaceStrategy(WhitespaceStrategy.ALWAYS);
		assertTrue(jsonFiltersProperties.getRequests().hasWhitespaceStrategy());
		
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
		assertEquals(f.getMaxSize(), Integer.MAX_VALUE);
		
		assertTrue(filter.isRemovingWhitespace());
		assertTrue(maxSizeFilter.isRemovingWhitespace());
	}
	

	@Test
	public void createFilterWithoutValidationAlwaysRemoveWhitespaceMaxSize() {
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
		jsonFiltersProperties.getRequests().setWhitespaceStrategy(WhitespaceStrategy.ALWAYS);
		assertTrue(jsonFiltersProperties.getRequests().hasWhitespaceStrategy());
		
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
		
		assertTrue(filter.isRemovingWhitespace());
		assertTrue(maxSizeFilter.isRemovingWhitespace());
	}

	@Test
	public void createFilterWithoutValidationNeverRemoveWhitespace() {
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
		jsonFiltersProperties.getRequests().setWhitespaceStrategy(WhitespaceStrategy.NEVER);
		
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
		assertEquals(f.getMaxSize(), Integer.MAX_VALUE);
		
		assertFalse(filter.isRemovingWhitespace());
		assertFalse(maxSizeFilter.isRemovingWhitespace());
	}

	@Test
	public void createFilterWithoutValidationNeverRemoveWhitespaceMaxSize() {
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
		jsonFiltersProperties.getRequests().setWhitespaceStrategy(WhitespaceStrategy.NEVER);
		
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
		
		assertFalse(filter.isRemovingWhitespace());
		assertFalse(maxSizeFilter.isRemovingWhitespace());
	}
	
	
	@Test
	public void testDisabled2() {
		JsonFiltersProperties properties = new JsonFiltersProperties();
		properties.setEnabled(false);
		
		Assertions.assertThrows(IllegalStateException.class, () -> {
			rrFactory.requestResponseJsonFilter(properties);
		});

	}
	
}
