package com.github.skjolber.jsonfilter.spring;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilterFactory;
import com.github.skjolber.jsonfilter.core.DefaultJsonFilterFactory;
import com.github.skjolber.jsonfilter.jackson.JacksonJsonFilterFactory;
import com.github.skjolber.jsonfilter.spring.matcher.AllJsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.spring.matcher.AntJsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.spring.matcher.JsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.spring.matcher.PrefixJsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.spring.properties.JsonFilterPathProperties;
import com.github.skjolber.jsonfilter.spring.properties.JsonFilterProperties;
import com.github.skjolber.jsonfilter.spring.properties.JsonFilterReplacementsProperties;
import com.github.skjolber.jsonfilter.spring.properties.JsonFiltersProperties;

@Configuration
@EnableConfigurationProperties({ JsonFiltersProperties.class })
@ConditionalOnProperty(name = { "jsonfilter.enabled" }, havingValue = "true", matchIfMissing = true)
public class JsonFilterAutoConfiguration {
	
	@Bean
	public RequestResponseJsonFilter requestResponseJsonFilter(JsonFiltersProperties properties) {
		
		JsonFilterReplacementsProperties replacements = properties.getReplacements();
		
		List<JsonFilterPathProperties> filters = properties.getPaths();

		List<JsonFilterPathMatcher> requestFilters = new ArrayList<JsonFilterPathMatcher>();
		List<JsonFilterPathMatcher> responseFilters = new ArrayList<JsonFilterPathMatcher>();
		
		AntPathMatcher matcher = new AntPathMatcher();
		
		for(JsonFilterPathProperties filter : filters) {
			JsonFilterProperties request = filter.getRequest();
			if(request.isEnabled()) {
				String antMatcher = filter.getAntMatcher();
				
				JsonFilter jsonFilter = createFilter(request, replacements);
				
				if(antMatcher == null) {
					requestFilters.add(new AllJsonFilterPathMatcher(jsonFilter));
				} else if(!matcher.isPattern(antMatcher)) {
					requestFilters.add(new PrefixJsonFilterPathMatcher(antMatcher, jsonFilter));
				} else {
					requestFilters.add(new AntJsonFilterPathMatcher(matcher, antMatcher, jsonFilter));
				}
			}
			JsonFilterProperties response = filter.getResponse();
			if(response.isEnabled()) {
				String antMatcher = filter.getAntMatcher();
				
				JsonFilter jsonFilter = createFilter(response, replacements);
				
				if(antMatcher == null) {
					requestFilters.add(new AllJsonFilterPathMatcher(jsonFilter));
				} else if(!matcher.isPattern(antMatcher)) {
					requestFilters.add(new PrefixJsonFilterPathMatcher(antMatcher, jsonFilter));
				} else {
					requestFilters.add(new AntJsonFilterPathMatcher(matcher, antMatcher, jsonFilter));
				}
			}
			
		}
		
		return new RequestResponseJsonFilter(requestFilters, responseFilters);
	}

	private JsonFilter createFilter(JsonFilterProperties request, JsonFilterReplacementsProperties replacements) {
		
		AbstractJsonFilterFactory factory;
		if(request.isCompact() || request.isValidate()) {
			// jackson
			factory = new JacksonJsonFilterFactory();
		} else {
			// core
			factory = new DefaultJsonFilterFactory();
		}
		
		if(replacements.hasAnonymize()) {
			factory.setAnonymizeStringValue(replacements.getAnonymize());
		}
		if(replacements.hasPrune()) {
			factory.setPruneStringValue(replacements.getPrune());
		}
		if(replacements.hasTruncate()) {
			factory.setTruncateStringValue(replacements.getTruncate());
		}

		factory.setAnonymizeFilters(request.getAnonymizes());
		factory.setPruneFilters(request.getPrunes());
		factory.setMaxStringLength(request.getMaxStringLength());
		factory.setMaxPathMatches(request.getMaxPathMatches());
		
		return factory.newJsonFilter();
	}
	
}
