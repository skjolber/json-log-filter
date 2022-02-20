package com.github.skjolber.jsonfilter.spring.autoconfigure.logbook;

import org.zalando.logbook.HttpResponse;

public class DefaultPreprocessedHttpResponse extends AbstractPreprocessedHttpMessage<HttpResponse>{

	public DefaultPreprocessedHttpResponse(HttpResponse message, boolean databindingPerformed, boolean databindingSuccessful) {
		super(message, databindingPerformed, databindingSuccessful);
	}


}
