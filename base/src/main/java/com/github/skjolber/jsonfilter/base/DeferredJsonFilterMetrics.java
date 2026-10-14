package com.github.skjolber.jsonfilter.base;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;

/**
 * Metrics which are collected first, and passed on to another {@linkplain JsonFilterMetrics}
 * only if the result is kept. Counts are summed; unknown counts (-1) are passed on as-is.
 */

public class DeferredJsonFilterMetrics implements JsonFilterMetrics {

	private int maxStringLength;
	private int maxStringLengthUnknown;
	private int maxSize;
	private int maxSizeUnknown;
	private int prune;
	private int pruneUnknown;
	private int anonymize;
	private int anonymizeUnknown;
	private int input;
	private int output;
	private boolean inputRecorded;
	private boolean outputRecorded;

	@Override
	public void onMaxStringLength(int count) {
		if(count == -1) {
			maxStringLengthUnknown++;
		} else {
			maxStringLength += count;
		}
	}

	@Override
	public void onMaxSize(int count) {
		if(count == -1) {
			maxSizeUnknown++;
		} else {
			maxSize += count;
		}
	}

	@Override
	public void onPrune(int count) {
		if(count == -1) {
			pruneUnknown++;
		} else {
			prune += count;
		}
	}

	@Override
	public void onAnonymize(int count) {
		if(count == -1) {
			anonymizeUnknown++;
		} else {
			anonymize += count;
		}
	}

	@Override
	public void onInput(int inputSize) {
		input += inputSize;
		inputRecorded = true;
	}

	@Override
	public void onOutput(int outputSize) {
		output += outputSize;
		outputRecorded = true;
	}

	/**
	 * Pass the collected metrics on.
	 *
	 * @param target metrics to pass on to
	 */

	public void flush(JsonFilterMetrics target) {
		if(maxStringLength > 0) {
			target.onMaxStringLength(maxStringLength);
		}
		for(int i = 0; i < maxStringLengthUnknown; i++) {
			target.onMaxStringLength(-1);
		}
		if(maxSize > 0) {
			target.onMaxSize(maxSize);
		}
		for(int i = 0; i < maxSizeUnknown; i++) {
			target.onMaxSize(-1);
		}
		if(prune > 0) {
			target.onPrune(prune);
		}
		for(int i = 0; i < pruneUnknown; i++) {
			target.onPrune(-1);
		}
		if(anonymize > 0) {
			target.onAnonymize(anonymize);
		}
		for(int i = 0; i < anonymizeUnknown; i++) {
			target.onAnonymize(-1);
		}
		if(inputRecorded) {
			target.onInput(input);
		}
		if(outputRecorded) {
			target.onOutput(output);
		}
	}
}
