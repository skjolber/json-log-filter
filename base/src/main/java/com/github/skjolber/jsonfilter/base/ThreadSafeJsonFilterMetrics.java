package com.github.skjolber.jsonfilter.base;

import java.util.concurrent.atomic.AtomicLong;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;

/**
 * Metrics which add up the counts and sizes of all filter operations, like {@link DefaultJsonFilterMetrics}, but which
 * can be shared between threads: the totals are {@link AtomicLong}s.
 * <br>
 * Each total is updated atomically, but the totals are not updated together, so while other threads are filtering,
 * the getters may return totals from slightly different points in time.
 * <br>
 * For less contention, filter with a {@link DefaultJsonFilterMetrics} per thread or per request, and add it up here
 * with {@link #add(DefaultJsonFilterMetrics)}.
 */

public class ThreadSafeJsonFilterMetrics implements JsonFilterMetrics {

	private final AtomicLong maxStringLength = new AtomicLong();
	private final AtomicLong maxSize = new AtomicLong();
	private final AtomicLong prune = new AtomicLong();
	private final AtomicLong anonymize = new AtomicLong();
	private final AtomicLong inputSize = new AtomicLong();
	private final AtomicLong outputSize = new AtomicLong();

	@Override
	public void onMaxStringLength(int count) {
		maxStringLength.addAndGet(count);
	}

	@Override
	public void onMaxSize(int count) {
		maxSize.addAndGet(count);
	}

	@Override
	public void onPrune(int count) {
		prune.addAndGet(count);
	}

	@Override
	public void onAnonymize(int count) {
		anonymize.addAndGet(count);
	}

	@Override
	public void onInput(int inputSize) {
		this.inputSize.addAndGet(inputSize);
	}

	@Override
	public void onOutput(int outputSize) {
		this.outputSize.addAndGet(outputSize);
	}

	/**
	 * Add the totals of other metrics, for example metrics collected per thread or per request.
	 *
	 * @param metrics metrics to add
	 */

	public void add(DefaultJsonFilterMetrics metrics) {
		add(metrics.getMaxStringLength(), metrics.getMaxSize(), metrics.getPrune(), metrics.getAnonymize(), metrics.getInputSize(), metrics.getOutputSize());
	}

	/**
	 * Add the totals of other metrics.
	 *
	 * @param metrics metrics to add
	 */

	public void add(ThreadSafeJsonFilterMetrics metrics) {
		add(metrics.getMaxStringLength(), metrics.getMaxSize(), metrics.getPrune(), metrics.getAnonymize(), metrics.getInputSize(), metrics.getOutputSize());
	}

	private void add(long maxStringLength, long maxSize, long prune, long anonymize, long inputSize, long outputSize) {
		this.maxStringLength.addAndGet(maxStringLength);
		this.maxSize.addAndGet(maxSize);
		this.prune.addAndGet(prune);
		this.anonymize.addAndGet(anonymize);
		this.inputSize.addAndGet(inputSize);
		this.outputSize.addAndGet(outputSize);
	}

	public long getAnonymize() {
		return anonymize.get();
	}

	public long getMaxSize() {
		return maxSize.get();
	}

	public long getMaxStringLength() {
		return maxStringLength.get();
	}

	public long getPrune() {
		return prune.get();
	}

	public long getInputSize() {
		return inputSize.get();
	}

	public long getOutputSize() {
		return outputSize.get();
	}

	@Override
	public String toString() {
		long input = getInputSize();
		long output = getOutputSize();

		StringBuilder builder = new StringBuilder(160);
		builder.append(getClass().getSimpleName());
		builder.append("[input size ").append(input);
		builder.append(", output size ").append(output);
		if(input > 0) {
			// how much of the input was written, to one decimal
			long permille = (output * 1000 + input / 2) / input;
			builder.append(" (").append(permille / 10).append('.').append(permille % 10).append("% of input)");
		}
		builder.append(", anonymized ").append(getAnonymize());
		builder.append(", pruned ").append(getPrune());
		builder.append(", strings truncated ").append(getMaxStringLength());
		builder.append(", left out by max size ").append(getMaxSize());
		builder.append(']');
		return builder.toString();
	}
}
