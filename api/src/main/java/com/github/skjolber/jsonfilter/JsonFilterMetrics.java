package com.github.skjolber.jsonfilter;

/**
 * 
 * Interface for recording the filtering operation
 *
 */

public interface JsonFilterMetrics {

	void onMaxStringLength(int removed);
	
	void onMaxSize(int removed);
	
	void onPrune(int count);
	
	void onAnonymize(int count);
}
