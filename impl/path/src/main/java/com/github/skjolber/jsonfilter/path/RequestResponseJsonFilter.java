package com.github.skjolber.jsonfilter.path;

import java.util.List;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.path.matcher.JsonFilterPathMatcher;

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

	public JsonFilter getRequestFilter(String path, boolean validate) {
		for(JsonFilterPathMatcher matcher : requests) {
			if(matcher.matches(path)) {
				return matcher.getFilter(validate);
			}
		}
		return null;
	}

	public JsonFilter getResponseFilter(String path, boolean validate) {
		for(JsonFilterPathMatcher matcher : responses) {
			if(matcher.matches(path)) {
				return matcher.getFilter(validate);
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
