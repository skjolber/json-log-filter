package com.github.skjolber.jsonfilter.spring.logbook;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.github.skjolber.jsonfilter.spring.autoconfigure.logbook.PreprocessedHttpResponse;
import com.github.skjolberg.jsonfilter.spring.logbook.servlet.LocalResponse;

public class PreprocessedLocalResponse extends LocalResponse implements PreprocessedHttpResponse {

	protected boolean databindingPerformed;
	protected boolean databindingSuccessful; 

	public PreprocessedLocalResponse(HttpServletResponse response, final String protocolVersion) {
		super(response, protocolVersion);
	}
	
	@Override
	public boolean isDatabindingPerformed() {
		return databindingPerformed;
	}

	@Override
	public boolean wasDatabindingSuccessful() {
		return databindingSuccessful;
	}
	
	public void setDatabindingPerformed(boolean databindingPerformed) {
		this.databindingPerformed = databindingPerformed;
	}
	
	public void setDatabindingSuccessful(boolean databindingSuccessful) {
		this.databindingSuccessful = databindingSuccessful;
	}
	
	@Override
	public PreprocessedLocalResponse withBody() throws IOException {
		return (PreprocessedLocalResponse)super.withBody();
	}
	
}
