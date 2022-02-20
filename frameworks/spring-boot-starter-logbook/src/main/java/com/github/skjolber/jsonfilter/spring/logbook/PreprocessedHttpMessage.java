package com.github.skjolber.jsonfilter.spring.logbook;

public interface PreprocessedHttpMessage {

	boolean isDatabindingPerformed();
	
	boolean wasDatabindingSuccessful();
}
