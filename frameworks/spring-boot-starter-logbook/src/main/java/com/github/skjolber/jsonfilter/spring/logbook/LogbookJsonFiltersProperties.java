package com.github.skjolber.jsonfilter.spring.logbook;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.github.skjolber.jsonfilter.path.properties.JsonFiltersProperties;

@ConfigurationProperties(prefix = "jsonfilter.logbook")
public class LogbookJsonFiltersProperties extends JsonFiltersProperties {

	protected JsonFiltersProperties filters;
	
	/** 
	 * Always compact the result. 
	 * If the request cannot be filtered, escape it as a string.
	 * In other words, the result can be appended as raw data to a JSON output (after a field name). 
	 */

	protected boolean compactRequests = true;
	protected boolean compactResponses;

	public boolean isCompactRequests() {
		return compactRequests;
	}
	
	public boolean isCompactResponses() {
		return compactResponses;
	}

	public void setCompactRequests(boolean compactRequests) {
		this.compactRequests = compactRequests;
	}
	
	public void setCompactResponses(boolean compactResponses) {
		this.compactResponses = compactResponses;
	}

}
