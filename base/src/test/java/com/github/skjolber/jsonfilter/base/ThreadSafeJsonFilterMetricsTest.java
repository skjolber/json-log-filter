package com.github.skjolber.jsonfilter.base;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

public class ThreadSafeJsonFilterMetricsTest {

	@Test
	public void testConcurrentUpdates() throws Exception {
		ThreadSafeJsonFilterMetrics metrics = new ThreadSafeJsonFilterMetrics();

		int threads = 8;
		int iterations = 10_000;

		ExecutorService executor = Executors.newFixedThreadPool(threads);
		try {
			CountDownLatch start = new CountDownLatch(1);
			List<Future<?>> futures = new ArrayList<>();
			for(int i = 0; i < threads; i++) {
				futures.add(executor.submit(() -> {
					start.await();
					for(int k = 0; k < iterations; k++) {
						metrics.onInput(3);
						metrics.onOutput(2);
						metrics.onAnonymize(1);
						metrics.onPrune(1);
						metrics.onMaxStringLength(1);
						metrics.onMaxSize(4);
					}
					return null;
				}));
			}
			start.countDown();
			for(Future<?> future : futures) {
				future.get();
			}
		} finally {
			executor.shutdownNow();
		}

		long total = (long)threads * iterations;
		assertThat(metrics.getInputSize()).isEqualTo(3 * total);
		assertThat(metrics.getOutputSize()).isEqualTo(2 * total);
		assertThat(metrics.getAnonymize()).isEqualTo(total);
		assertThat(metrics.getPrune()).isEqualTo(total);
		assertThat(metrics.getMaxStringLength()).isEqualTo(total);
		assertThat(metrics.getMaxSize()).isEqualTo(4 * total);
	}

	@Test
	public void testAdd() {
		DefaultJsonFilterMetrics perRequest = new DefaultJsonFilterMetrics();
		perRequest.onInput(100);
		perRequest.onOutput(80);
		perRequest.onAnonymize(2);
		perRequest.onPrune(1);
		perRequest.onMaxStringLength(3);
		perRequest.onMaxSize(15);

		ThreadSafeJsonFilterMetrics metrics = new ThreadSafeJsonFilterMetrics();
		metrics.add(perRequest);
		metrics.add(perRequest);

		assertThat(metrics.getInputSize()).isEqualTo(200);
		assertThat(metrics.getOutputSize()).isEqualTo(160);
		assertThat(metrics.getAnonymize()).isEqualTo(4);
		assertThat(metrics.getPrune()).isEqualTo(2);
		assertThat(metrics.getMaxStringLength()).isEqualTo(6);
		assertThat(metrics.getMaxSize()).isEqualTo(30);

		ThreadSafeJsonFilterMetrics total = new ThreadSafeJsonFilterMetrics();
		total.onInput(1);
		total.add(metrics);
		assertThat(total.getInputSize()).isEqualTo(201);
		assertThat(total.getMaxSize()).isEqualTo(30);
	}

	@Test
	public void testToString() {
		ThreadSafeJsonFilterMetrics metrics = new ThreadSafeJsonFilterMetrics();
		assertThat(metrics.toString()).isEqualTo("ThreadSafeJsonFilterMetrics[input size 0, output size 0, anonymized 0, pruned 0, strings truncated 0, left out by max size 0]");

		metrics.onInput(2000);
		metrics.onOutput(1499);
		metrics.onAnonymize(3);
		metrics.onPrune(1);
		metrics.onMaxStringLength(7);
		metrics.onMaxSize(400);
		assertThat(metrics.toString()).isEqualTo("ThreadSafeJsonFilterMetrics[input size 2000, output size 1499 (75.0% of input), anonymized 3, pruned 1, strings truncated 7, left out by max size 400]");
	}
}
