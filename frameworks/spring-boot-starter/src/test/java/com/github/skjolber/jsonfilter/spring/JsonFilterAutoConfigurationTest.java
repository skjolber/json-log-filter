package com.github.skjolber.jsonfilter.spring;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonJsonFilter;
import com.github.skjolber.jsonfilter.spring.matcher.AllJsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.spring.matcher.JsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.spring.matcher.PrefixJsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.spring.properties.JsonFilterProperties;
import com.github.skjolber.jsonfilter.spring.properties.JsonFilterReplacementsProperties;
import com.github.skjolber.jsonfilter.spring.properties.JsonFiltersProperties;

public class JsonFilterAutoConfigurationTest {

	@Test
	public void isGuardedByProperty() {
		JsonFilterAutoConfiguration c = new JsonFilterAutoConfiguration();
		
		JsonFiltersProperties properties = new JsonFiltersProperties();
		c.requestResponseJsonFilter(properties);
		
		assertTrue(c.toFilter(null,  null,  null) instanceof AllJsonFilterPathMatcher);
		
		AntPathMatcher someAntPathMatcher = new AntPathMatcher("#");
		
		assertTrue(c.toFilter(someAntPathMatcher, "/ABC", null) instanceof PrefixJsonFilterPathMatcher);
		
	}
	
	@Test 
	public void testAllForEmptyMatcher() {
		
		JsonFilter filter = new DefaultJsonFilter();
		
		JsonFilterPathMatcher matcher = new JsonFilterAutoConfiguration().toFilter(null, null, filter);
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
		replacements.setPrune("a");
		replacements.setAnonymize("b");
		replacements.setTruncate("c");

		JsonFilter filter = JsonFilterAutoConfiguration.createFilter(request, replacements);
		
		assertTrue(filter instanceof JacksonJsonFilter);
	}
}
