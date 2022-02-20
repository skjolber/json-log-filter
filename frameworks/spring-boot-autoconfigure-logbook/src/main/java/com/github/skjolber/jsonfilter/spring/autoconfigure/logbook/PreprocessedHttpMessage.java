package com.github.skjolber.jsonfilter.spring.autoconfigure.logbook;

import org.zalando.logbook.HttpMessage;

public interface PreprocessedHttpMessage extends HttpMessage {

	boolean isDatabindingPerformed();
	
	boolean wasDatabindingSuccessful();
}
