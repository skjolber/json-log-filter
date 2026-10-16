package com.github.skjolber.jsonfilter.base;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

public class DefaultJsonFilterMetricsTest {

	@Test
	public void testIncrements() {
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		
		metrics.onAnonymize(1);
		assertEquals(metrics.getAnonymize(), 1);
		
		metrics.onInput(2);
		assertEquals(metrics.getInputSize(), 2);
		
		metrics.onMaxSize(3);
		assertEquals(metrics.getMaxSize(), 3);
		
		metrics.onMaxStringLength(4);
		assertEquals(metrics.getMaxStringLength(), 4);
		
		metrics.onOutput(5);
		assertEquals(metrics.getOutputSize(), 5);
		
		metrics.onPrune(6);
		assertEquals(metrics.getPrune(), 6);
	}

	@Test
	public void testTotalsBeyondIntRange() {
		// the totals are long, so they keep counting beyond about 2 GB of input
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		for(int i = 0; i < 3; i++) {
			metrics.onAnonymize(Integer.MAX_VALUE);
			metrics.onInput(Integer.MAX_VALUE);
			metrics.onMaxSize(Integer.MAX_VALUE);
			metrics.onMaxStringLength(Integer.MAX_VALUE);
			metrics.onOutput(Integer.MAX_VALUE);
			metrics.onPrune(Integer.MAX_VALUE);
		}
		long expected = 3L * Integer.MAX_VALUE;
		assertEquals(expected, metrics.getAnonymize());
		assertEquals(expected, metrics.getInputSize());
		assertEquals(expected, metrics.getMaxSize());
		assertEquals(expected, metrics.getMaxStringLength());
		assertEquals(expected, metrics.getOutputSize());
		assertEquals(expected, metrics.getPrune());
	}
}
