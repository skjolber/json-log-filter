package com.github.skjolber.jsonfilter.base;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;

public class DeferredJsonFilterMetricsTest {

	@Test
	public void testFlushPassesOnSums() {
		DeferredJsonFilterMetrics metrics = new DeferredJsonFilterMetrics();
		metrics.onMaxStringLength(1);
		metrics.onMaxStringLength(2);
		metrics.onMaxSize(3);
		metrics.onMaxSize(4);
		metrics.onPrune(5);
		metrics.onPrune(6);
		metrics.onAnonymize(7);
		metrics.onAnonymize(8);
		metrics.onInput(9);
		metrics.onOutput(10);

		JsonFilterMetrics target = mock(JsonFilterMetrics.class);
		metrics.flush(target);

		verify(target).onMaxStringLength(3);
		verify(target).onMaxSize(7);
		verify(target).onPrune(11);
		verify(target).onAnonymize(15);
		verify(target).onInput(9);
		verify(target).onOutput(10);
		verifyNoMoreInteractions(target);
	}

	@Test
	public void testFlushPassesOnUnknownCountsAsIs() {
		// -1 means an unknown count, so it cannot be summed
		DeferredJsonFilterMetrics metrics = new DeferredJsonFilterMetrics();
		metrics.onMaxStringLength(-1);
		metrics.onMaxSize(-1);
		metrics.onMaxSize(-1);
		metrics.onPrune(2);
		metrics.onPrune(-1);
		metrics.onAnonymize(-1);

		JsonFilterMetrics target = mock(JsonFilterMetrics.class);
		metrics.flush(target);

		verify(target).onMaxStringLength(-1);
		verify(target, times(2)).onMaxSize(-1);
		verify(target).onPrune(2);
		verify(target).onPrune(-1);
		verify(target).onAnonymize(-1);
		verifyNoMoreInteractions(target);
	}

	@Test
	public void testFlushPassesOnRecordedInputAndOutputOnly() {
		JsonFilterMetrics target = mock(JsonFilterMetrics.class);
		new DeferredJsonFilterMetrics().flush(target);
		verifyNoInteractions(target);

		// an empty input or output is still passed on
		DeferredJsonFilterMetrics metrics = new DeferredJsonFilterMetrics();
		metrics.onInput(0);
		metrics.onOutput(0);

		metrics.flush(target);

		verify(target).onInput(0);
		verify(target).onOutput(0);
		verifyNoMoreInteractions(target);
	}
}
