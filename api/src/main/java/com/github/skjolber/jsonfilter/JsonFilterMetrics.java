package com.github.skjolber.jsonfilter;

/**
 * 
 * Interface for recording the filtering operation.
 * <br>
 * Metrics are recorded while filtering, so they are also updated if filtering is not successful,
 * i.e. the filter returns false or null. It is then up to the caller to reset them.
 *
 */

public interface JsonFilterMetrics {

	/**
	 * Record string value truncated
	 * 
	 * @param count number of values, or -1 if unknown count
	 */
	
	void onMaxStringLength(int count);
	
	/**
	 * Record document truncated by max size
	 * 
	 * @param count approximate size of the input left out, in characters or bytes like the input size, or -1 if unknown
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

	/**
	 * Record filter operation
	 * 
	 * @param inputSize total input size in characters or bytes
	 */
	
	void onInput(int inputSize);

	/**
	 * Record filter operation
	 * 
	 * @param outputSize total output size in characters or bytes
	 */
	
	void onOutput(int outputSize);
}
