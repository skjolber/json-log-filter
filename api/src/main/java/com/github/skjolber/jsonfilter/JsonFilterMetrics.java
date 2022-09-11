package com.github.skjolber.jsonfilter;

/**
 * 
 * Interface for recording the filtering operation
 *
 */

public interface JsonFilterMetrics {

	/**
	 * Record string value truncated
	 * 
	 * @param count number of characters or bytes truncated, or -1 if unknown
	 */
	
	void onMaxStringLength(int count);
	
	/**
	 * Record document truncated
	 * 
	 * @param count number of characters or bytes truncated, or -1 if unknown
	 */
	
	void onMaxSize(int count);
	
	/**
	 * Record value(s) pruned (deleted)
	 * 
	 * @param count number of values, or -1 if unknown count
	 */
	
	void onPrune(int count);
	
	/**
	 * Record value(s) anonymized
	 * 
	 * @param count number of values, or -1 if unknown count
	 */
	
	void onAnonymize(int count);
}
