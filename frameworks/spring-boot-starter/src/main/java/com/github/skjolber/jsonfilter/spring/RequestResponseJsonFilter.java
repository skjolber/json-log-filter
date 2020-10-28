package com.github.skjolber.jsonfilter.spring;

import java.util.List;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.spring.matcher.JsonFilterPathMatcher;

public class RequestResponseJsonFilter {

	protected final JsonFilterPathMatcher[] requests;
	protected final JsonFilterPathMatcher[] responses;

	public RequestResponseJsonFilter(List<JsonFilterPathMatcher> requests, List<JsonFilterPathMatcher> responses) {
		this(requests.toArray(new JsonFilterPathMatcher[requests.size()]), responses.toArray(new JsonFilterPathMatcher[responses.size()]));
	}

	public RequestResponseJsonFilter(JsonFilterPathMatcher[] requests, JsonFilterPathMatcher[] responses) {
		this.requests = requests;
		this.responses = responses;
	}

	public JsonFilter getRequestFilter(String path) {
		for(JsonFilterPathMatcher matcher : requests) {
			if(matcher.matches(path)) {
				return matcher.getFilter();
			}
		}
		return null;
	}

	public JsonFilter getResponseFilter(String path) {
		for(JsonFilterPathMatcher matcher : responses) {
			if(matcher.matches(path)) {
				return matcher.getFilter();
			}
		}
		return null;
	}
	
	protected JsonFilterPathMatcher[] getRequests() {
		return requests;
	}
	
	protected JsonFilterPathMatcher[] getResponses() {
		return responses;
	}

}
