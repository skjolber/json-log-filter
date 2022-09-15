package com.github.skjolber.jsonfilter.base;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;

public class DefaultJsonFilterMetrics implements JsonFilterMetrics {

	private int maxStringLength;
	private int maxSize;
	private int prune;
	private int anonymize;
	private int inputSize;
	private int outputSize;
	
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
	
	public int getAnonymize() {
		return anonymize;
	}
	
	public int getMaxSize() {
		return maxSize;
	}
	
	public int getMaxStringLength() {
		return maxStringLength;
	}
	
	public int getPrune() {
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
	
	public int getInputSize() {
		return inputSize;
	}
	
	public int getOutputSize() {
		return outputSize;
	}

}
