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
}
