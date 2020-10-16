package com.github.skjolber.jsonfilter.spring;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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
		if(!properties.isEnabled()) {
			throw new IllegalStateException();
		}
		JsonFilterReplacementsProperties replacements = properties.getReplacements();
		
		List<JsonFilterPathProperties> filters = properties.getPaths();

		AntPathMatcher matcher = new AntPathMatcher();
		
		List<JsonFilterPathMatcher> requestFilters = extract(matcher, replacements, filters, (f) -> f.getRequest());
		List<JsonFilterPathMatcher> responseFilters = extract(matcher, replacements, filters, (f) -> f.getResponse());
		
		return new RequestResponseJsonFilter(requestFilters, responseFilters);
	}
	
	protected List<JsonFilterPathMatcher> extract(AntPathMatcher matcher, JsonFilterReplacementsProperties replacements, List<JsonFilterPathProperties> filters, Function<JsonFilterPathProperties, JsonFilterProperties> mapper) {
		List<JsonFilterPathMatcher> requestFilters = new ArrayList<JsonFilterPathMatcher>();
		for(JsonFilterPathProperties filter : filters) {
			JsonFilterProperties request = mapper.apply(filter);
			if(request.isEnabled()) {
				String antMatcher = filter.getAntMatcher();
				
				JsonFilter jsonFilter = createFilter(request, replacements);
				
				JsonFilterPathMatcher m = toFilter(matcher, antMatcher, jsonFilter);
				
				requestFilters.add(m);
			}
		}
		return requestFilters;
	}

	protected JsonFilterPathMatcher toFilter(AntPathMatcher matcher, String antMatcher, JsonFilter jsonFilter) {
		JsonFilterPathMatcher m; 
		if(antMatcher == null || antMatcher.isEmpty()) {
			m = new AllJsonFilterPathMatcher(jsonFilter);
		} else if(!matcher.isPattern(antMatcher)) {
			m = new PrefixJsonFilterPathMatcher(antMatcher, jsonFilter);
		} else {
			m = new AntJsonFilterPathMatcher(matcher, antMatcher, jsonFilter);
		}
		return m;
	}

	protected static JsonFilter createFilter(JsonFilterProperties request, JsonFilterReplacementsProperties replacements) {
		
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
