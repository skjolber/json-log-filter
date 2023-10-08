package com.github.skjolber.jsonfilter.path;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterFactory;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilterFactory;
import com.github.skjolber.jsonfilter.core.DefaultJsonFilterFactory;
import com.github.skjolber.jsonfilter.jackson.JacksonJsonFilterFactory;
import com.github.skjolber.jsonfilter.path.matcher.JsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.path.matcher.JsonFilterPathMatcherFactory;
import com.github.skjolber.jsonfilter.path.properties.JsonFilterPathProperties;
import com.github.skjolber.jsonfilter.path.properties.JsonFilterProperties;
import com.github.skjolber.jsonfilter.path.properties.JsonFilterReplacementsProperties;
import com.github.skjolber.jsonfilter.path.properties.JsonFiltersProperties;
import com.github.skjolber.jsonfilter.path.properties.ProcessingProperties;
import com.github.skjolber.jsonfilter.path.properties.WhitespaceStrategy;

public class RequestResponseJsonFilterFactory {

	protected final JsonFilterPathMatcherFactory jsonFilterPathMatcherFactory;
	
	public RequestResponseJsonFilterFactory(JsonFilterPathMatcherFactory jsonFilterPathMatcherFactory) {
		this.jsonFilterPathMatcherFactory = jsonFilterPathMatcherFactory;
	}

	public RequestResponseJsonFilter requestResponseJsonFilter(JsonFiltersProperties properties) {
		if(!properties.isEnabled()) {
			throw new IllegalStateException();
		}
		JsonFilterReplacementsProperties replacements = properties.getReplacements();

		List<JsonFilterPathProperties> filters = properties.getPaths();

		List<JsonFilterPathMatcher> requestFilters = extract(replacements, filters, (f) -> f.getRequest(), properties.getRequests());
		List<JsonFilterPathMatcher> responseFilters = extract(replacements, filters, (f) -> f.getResponse(), properties.getResponses());

		return new RequestResponseJsonFilter(requestFilters, responseFilters);
	}

	protected List<JsonFilterPathMatcher> extract(JsonFilterReplacementsProperties replacements, List<JsonFilterPathProperties> filters, Function<JsonFilterPathProperties, JsonFilterProperties> mapper, ProcessingProperties processingProperties) {
		List<JsonFilterPathMatcher> requestFilters = new ArrayList<JsonFilterPathMatcher>();
		for(JsonFilterPathProperties filter : filters) {
			JsonFilterProperties properties = mapper.apply(filter);
			if(properties != null && properties.isEnabled()) {
				String antMatcher = filter.getMatcher();

				JacksonJsonFilterFactory jacksonJsonFilterFactory = new JacksonJsonFilterFactory();
				configureFactory(jacksonJsonFilterFactory, properties, replacements);
				
				DefaultJsonFilterFactory nonvalidating = new DefaultJsonFilterFactory();
				configureFactory(nonvalidating, properties, replacements);

				WhitespaceStrategy whitespaceStrategy = processingProperties.getWhitespaceStrategy();

				int maxSize = properties.getMaxSize();
				if(maxSize == -1) {
					maxSize = processingProperties.getMaxSize();
				}

				if(maxSize == -1) {
					if(whitespaceStrategy == WhitespaceStrategy.NEVER) {
						JsonFilter validatingFilter = jacksonJsonFilterFactory.newJsonFilter();
						JsonFilter nonvalidatingFilter = nonvalidating.newJsonFilter();
	
						JsonFilterPathMatcher m = jsonFilterPathMatcherFactory.createMatcher(antMatcher, validatingFilter, null, nonvalidatingFilter, null, Integer.MAX_VALUE);
						requestFilters.add(m);
					} else if(whitespaceStrategy == WhitespaceStrategy.ON_DEMAND) {
						JsonFilter validatingFilter = jacksonJsonFilterFactory.newJsonFilter();
						JsonFilter nonvalidatingFilter = nonvalidating.newJsonFilter();
						
						jacksonJsonFilterFactory.setRemoveWhitespace(true);
						nonvalidating.setRemoveWhitespace(true);

						JsonFilter validatingRemoveWhitespaceFilter = jacksonJsonFilterFactory.newJsonFilter();
						JsonFilter nonvalidatingRemoveWhitespaceFilter = nonvalidating.newJsonFilter();
						
						JsonFilterPathMatcher m = jsonFilterPathMatcherFactory.createMatcher(antMatcher, validatingFilter, validatingRemoveWhitespaceFilter, nonvalidatingFilter, nonvalidatingRemoveWhitespaceFilter, Integer.MAX_VALUE);
						requestFilters.add(m);
					} else if(whitespaceStrategy == WhitespaceStrategy.ALWAYS) {
						jacksonJsonFilterFactory.setRemoveWhitespace(true);
						nonvalidating.setRemoveWhitespace(true);

						JsonFilter validatingFilter = jacksonJsonFilterFactory.newJsonFilter();
						JsonFilter nonvalidatingFilter = nonvalidating.newJsonFilter();
						
						JsonFilterPathMatcher m = jsonFilterPathMatcherFactory.createMatcher(antMatcher, validatingFilter, null, nonvalidatingFilter, null, Integer.MAX_VALUE);
						requestFilters.add(m);
					}
				} else {
					if(whitespaceStrategy == WhitespaceStrategy.NEVER) {
						JsonFilter validatingFilter = jacksonJsonFilterFactory.newJsonFilter();
						JsonFilter nonvalidatingFilter = nonvalidating.newJsonFilter();

						jacksonJsonFilterFactory.setMaxSize(maxSize);
						nonvalidating.setMaxSize(maxSize);

						JsonFilter validatingMaxSizeFilter = jacksonJsonFilterFactory.newJsonFilter();
						JsonFilter nonvalidatingMaxSizeFilter = nonvalidating.newJsonFilter();

						JsonFilterPathMatcher m = jsonFilterPathMatcherFactory.createMatcher(antMatcher, validatingFilter, validatingMaxSizeFilter, nonvalidatingFilter, nonvalidatingMaxSizeFilter, maxSize);
						requestFilters.add(m);
					} else if(whitespaceStrategy == WhitespaceStrategy.ON_DEMAND) {
						JsonFilter validatingFilter = jacksonJsonFilterFactory.newJsonFilter();
						JsonFilter nonvalidatingFilter = nonvalidating.newJsonFilter();

						jacksonJsonFilterFactory.setMaxSize(maxSize);
						nonvalidating.setMaxSize(maxSize);

						jacksonJsonFilterFactory.setRemoveWhitespace(true);
						nonvalidating.setRemoveWhitespace(true);
						
						JsonFilter validatingRemoveWhitespaceFilter = jacksonJsonFilterFactory.newJsonFilter();
						JsonFilter nonvalidatingRemoveWhitespaceFilter = nonvalidating.newJsonFilter();
						
						JsonFilterPathMatcher m = jsonFilterPathMatcherFactory.createMatcher(antMatcher, validatingFilter, validatingRemoveWhitespaceFilter, nonvalidatingFilter, nonvalidatingRemoveWhitespaceFilter, maxSize);
						requestFilters.add(m);
					} else if(whitespaceStrategy == WhitespaceStrategy.ALWAYS) {
						jacksonJsonFilterFactory.setRemoveWhitespace(true);
						nonvalidating.setRemoveWhitespace(true);

						JsonFilter validatingFilter = jacksonJsonFilterFactory.newJsonFilter();
						JsonFilter nonvalidatingFilter = nonvalidating.newJsonFilter();

						jacksonJsonFilterFactory.setMaxSize(maxSize);
						nonvalidating.setMaxSize(maxSize);

						JsonFilter validatingRemoveWhitespaceFilter = jacksonJsonFilterFactory.newJsonFilter();
						JsonFilter nonvalidatingRemoveWhitespaceFilter = nonvalidating.newJsonFilter();
						
						JsonFilterPathMatcher m = jsonFilterPathMatcherFactory.createMatcher(antMatcher, validatingFilter, validatingRemoveWhitespaceFilter, nonvalidatingFilter, nonvalidatingRemoveWhitespaceFilter, maxSize);
						requestFilters.add(m);
					}
				}
				
			}
		}
		return requestFilters;
	}


	protected JsonFilterFactory configureFactory(AbstractJsonFilterFactory factory, JsonFilterProperties request, JsonFilterReplacementsProperties replacements) {

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

		return factory;
	}

}
