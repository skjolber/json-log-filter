package com.github.skjolber.jsonfilter.path.properties;

public class JsonFilterPathProperties {

	private String matcher;

	private JsonFilterProperties request;
	private JsonFilterProperties response;

	public String getMatcher() {
		return matcher;
	}

	public void setMatcher(String antMatcher) {
		this.matcher = antMatcher;
	}

	public JsonFilterProperties getRequest() {
		return request;
	}

	public void setRequest(JsonFilterProperties request) {
		this.request = request;
	}

	public JsonFilterProperties getResponse() {
		return response;
	}

	public void setResponse(JsonFilterProperties response) {
		this.response = response;
	}

	
}
