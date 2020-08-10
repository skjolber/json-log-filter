package com.github.skjolber.jsonfilter.spring.properties;

public class JsonFilterPathProperties {

	private boolean enabled = true;
	
	private String antMatcher;

	private JsonFilterProperties request;
	
	private JsonFilterProperties response;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getAntMatcher() {
		return antMatcher;
	}

	public void setAntMatcher(String antMatcher) {
		this.antMatcher = antMatcher;
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
