package com.github.skjolber.jsonfilter.spring.logbook;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.github.skjolber.jsonfilter.spring.autoconfigure.logbook.PreprocessedHttpRequest;
import com.github.skjolberg.jsonfilter.spring.logbook.servlet.RemoteRequest;

public class PreprocessedRemoteRequest extends RemoteRequest implements PreprocessedHttpRequest {

	protected boolean databindingPerformed;
	protected boolean databindingSuccessful; 

	public PreprocessedRemoteRequest(HttpServletRequest request) {
		super(request);
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
	public PreprocessedRemoteRequest withBody() throws IOException {
		return (PreprocessedRemoteRequest) super.withBody();
	}
}
