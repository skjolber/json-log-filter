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

public class RequestResponseJsonFilterFactory {

	protected final JsonFilterPathMatcherFactory jsonFilterPathMatcherFactory;
	protected final boolean validateRequests;
	protected final boolean validateResponses;
	
	public RequestResponseJsonFilterFactory(JsonFilterPathMatcherFactory jsonFilterPathMatcherFactory, boolean validateRequests, boolean validateResponses) {
		this.jsonFilterPathMatcherFactory = jsonFilterPathMatcherFactory;
		this.validateRequests = validateRequests;
		this.validateResponses = validateResponses;
	}

	public RequestResponseJsonFilter requestResponseJsonFilter(JsonFiltersProperties properties) {
		if(!properties.isEnabled()) {
			throw new IllegalStateException();
		}
		JsonFilterReplacementsProperties replacements = properties.getReplacements();

		List<JsonFilterPathProperties> filters = properties.getPaths();

		List<JsonFilterPathMatcher> requestFilters = extract(replacements, filters, (f) -> f.getRequest(), validateRequests);
		List<JsonFilterPathMatcher> responseFilters = extract(replacements, filters, (f) -> f.getResponse(), validateResponses);

		return new RequestResponseJsonFilter(requestFilters, responseFilters);
	}

	protected List<JsonFilterPathMatcher> extract(JsonFilterReplacementsProperties replacements, List<JsonFilterPathProperties> filters, Function<JsonFilterPathProperties, JsonFilterProperties> mapper, boolean validate) {
		List<JsonFilterPathMatcher> requestFilters = new ArrayList<JsonFilterPathMatcher>();
		for(JsonFilterPathProperties filter : filters) {
			JsonFilterProperties properties = mapper.apply(filter);
			if(properties != null && properties.isEnabled()) {
				String antMatcher = filter.getMatcher();

				JsonFilterFactory factory = createFactory(properties, replacements, validate);

				JsonFilter jsonFilter = factory.newJsonFilter();

				JsonFilterPathMatcher m = jsonFilterPathMatcherFactory.createMatcher(antMatcher, jsonFilter);

				requestFilters.add(m);
			}
		}
		return requestFilters;
	}


	protected JsonFilterFactory createFactory(JsonFilterProperties request, JsonFilterReplacementsProperties replacements, boolean validate) {

		AbstractJsonFilterFactory factory;
		if(request.isValidate() || validate) {
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

		return factory;
	}

}
