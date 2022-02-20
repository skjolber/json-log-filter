package com.github.skjolber.jsonfilter.spring.autoconfigure.logbook;

import org.zalando.logbook.HttpRequest;

public class DefaultPreprocessedHttpRequest extends AbstractPreprocessedHttpMessage<HttpRequest>{

	public DefaultPreprocessedHttpRequest(HttpRequest message, boolean databindingPerformed, boolean databindingSuccessful) {
		super(message, databindingPerformed, databindingSuccessful);
	}

}
