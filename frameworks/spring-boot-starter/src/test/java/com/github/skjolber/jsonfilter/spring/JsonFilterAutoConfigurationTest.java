package com.github.skjolber.jsonfilter.spring;


import static org.junit.jupiter.api.Assertions.*;
import static com.google.common.truth.Truth.*;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.core.DefaultJsonFilterFactory;
import com.github.skjolber.jsonfilter.jackson.JacksonJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonJsonFilterFactory;
import com.github.skjolber.jsonfilter.spring.matcher.AllJsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.spring.matcher.AntJsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.spring.matcher.JsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.spring.matcher.PrefixJsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.spring.properties.JsonFilterPathProperties;
import com.github.skjolber.jsonfilter.spring.properties.JsonFilterProperties;
import com.github.skjolber.jsonfilter.spring.properties.JsonFilterReplacementsProperties;
import com.github.skjolber.jsonfilter.spring.properties.JsonFiltersProperties;

public class JsonFilterAutoConfigurationTest {

	@Test
	public void testAutoconfiguration() {
		JsonFilterAutoConfiguration c = new JsonFilterAutoConfiguration();
		
		JsonFiltersProperties properties = new JsonFiltersProperties();
		c.requestResponseJsonFilter(properties);
		
		assertTrue(JsonFilterAutoConfiguration.toFilter(null,  null,  null) instanceof AllJsonFilterPathMatcher);
		assertTrue(JsonFilterAutoConfiguration.toFilter(null,  "",  null) instanceof AllJsonFilterPathMatcher);
		
		AntPathMatcher someAntPathMatcher = new AntPathMatcher("#");
		
		assertTrue(JsonFilterAutoConfiguration.toFilter(someAntPathMatcher, "/ABC", null) instanceof PrefixJsonFilterPathMatcher);
		assertTrue(JsonFilterAutoConfiguration.toFilter(new AntPathMatcher("/ABC*"), "/ABC*", null) instanceof AntJsonFilterPathMatcher);
		
		
		JsonFilterPathProperties p = new JsonFilterPathProperties();
	
		JsonFilterProperties request = new JsonFilterProperties();
		request.setEnabled(false);
		request.setValidate(true);
		request.setCompact(true);
		request.setAnonymizes(Arrays.asList("/a"));
		request.setPrunes(Arrays.asList("/b"));
		request.setMaxPathMatches(1);
		request.setEnabled(true);
		
		p.setRequest(request);
	}
	
	@Test
	public void testEnabled() {
		JsonFilterAutoConfiguration c = new JsonFilterAutoConfiguration();
		
		JsonFiltersProperties properties = new JsonFiltersProperties();
		
		JsonFilterPathProperties p = new JsonFilterPathProperties();
	
		JsonFilterProperties request = new JsonFilterProperties();
		request.setEnabled(false);
		request.setValidate(true);
		request.setCompact(true);
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
		
		JsonFilterPathMatcher matcher = JsonFilterAutoConfiguration.toFilter(null, null, filter);
		assertTrue(matcher instanceof AllJsonFilterPathMatcher);
	}
	
	@Test
	public void createFilterWithCustomJson() {
		JsonFilterProperties request = new JsonFilterProperties();
		request.setValidate(true);
		request.setCompact(true);
		request.setAnonymizes(Arrays.asList("/a"));
		request.setPrunes(Arrays.asList("/b"));
		request.setMaxPathMatches(1);
		request.setEnabled(true);
		
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
		
		JsonFilter filter = JsonFilterAutoConfiguration.createFactory(request, replacements).newJsonFilter();
		
		assertTrue(filter instanceof JacksonJsonFilter);
	}
	
	@Test
	public void test() {
		JsonFiltersProperties properties = new JsonFiltersProperties();
		properties.setEnabled(false);
		JsonFilterAutoConfiguration c = new JsonFilterAutoConfiguration();
		
		Assertions.assertThrows(IllegalStateException.class, () -> {
			c.requestResponseJsonFilter(properties);
		});

	}
	
	@Test
	public void isJacksonForCompactAndValidate() {
		JsonFilterReplacementsProperties replacements = new JsonFilterReplacementsProperties();
		
		JsonFilterProperties request = new JsonFilterProperties();
		assertThat(JsonFilterAutoConfiguration.createFactory(request, replacements)).isInstanceOf(DefaultJsonFilterFactory.class);
	
		request = new JsonFilterProperties();
		request.setValidate(true);
		assertThat(JsonFilterAutoConfiguration.createFactory(request, replacements)).isInstanceOf(JacksonJsonFilterFactory.class);
		
		request = new JsonFilterProperties();
		request.setCompact(true);
		assertThat(JsonFilterAutoConfiguration.createFactory(request, replacements)).isInstanceOf(JacksonJsonFilterFactory.class);
		
		request = new JsonFilterProperties();
		request.setValidate(true);
		request.setCompact(true);
		assertThat(JsonFilterAutoConfiguration.createFactory(request, replacements)).isInstanceOf(JacksonJsonFilterFactory.class);
	}
	
}
