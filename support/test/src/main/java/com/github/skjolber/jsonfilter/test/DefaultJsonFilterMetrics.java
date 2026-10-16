package com.github.skjolber.jsonfilter.test;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;

/**
 * Metrics which add up the counts and sizes of all filter operations. The totals are long, so that they do not
 * overflow after about 2 GB of input, while each operation reports int values.
 */

public class DefaultJsonFilterMetrics implements JsonFilterMetrics {

	private long maxStringLength;
	private long maxSize;
	private long prune;
	private long anonymize;
	private long inputSize;
	private long outputSize;
	
	@Override
	public void onMaxStringLength(int count) {
		maxStringLength += count;
	}

	@Override
	public void onMaxSize(int count) {
		maxSize += count;
	}

	@Override
	public void onPrune(int count) {
		prune += count;
	}

	@Override
	public void onAnonymize(int count) {
		anonymize += count;
	}
	
	public long getAnonymize() {
		return anonymize;
	}
	
	public long getMaxSize() {
		return maxSize;
	}
	
	public long getMaxStringLength() {
		return maxStringLength;
	}
	
	public long getPrune() {
		return prune;
	}

	@Override
	public void onInput(int inputSize) {
		this.inputSize += inputSize;
	}
	
	@Override
	public void onOutput(int outputSize) {
		this.outputSize += outputSize;
	}
	
	public long getInputSize() {
		return inputSize;
	}
	
	public long getOutputSize() {
		return outputSize;
	}
}
