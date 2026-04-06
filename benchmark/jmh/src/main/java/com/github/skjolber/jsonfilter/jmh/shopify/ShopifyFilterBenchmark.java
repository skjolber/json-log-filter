package com.github.skjolber.jsonfilter.jmh.shopify;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.AnyPathJsonFilter;
import com.github.skjolber.jsonfilter.core.DefaultJsonLogFilterBuilder;
import com.github.skjolber.jsonfilter.core.FullPathJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxStringLengthMaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.RemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.jmh.BenchmarkRunner;

/**
 * Benchmarks JSON filtering throughput on Shopify e-commerce order data.
 * Used to validate that throughput improvements generalize to real-world
 * e-commerce JSON structures with PII fields.
 *
 * Dataset: shopify_orders.json.gz (200 Shopify orders, compact JSON)
 * Top-level structure: {"orders": [...]}
 *
 * Filter paths:
 *   anonymize: /orders/email, /orders/billing_address/phone (PII)
 *   prune:     /orders/line_items, /orders/refunds (detailed order contents)
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ShopifyFilterBenchmark {

    private final int maxStringLength = 64;

    private static String[] anon = new String[]{"/orders/email", "/orders/billing_address/phone"};
    private static String[] prune = new String[]{"/orders/line_items", "/orders/refunds"};
    private static String[] anyPathFields = new String[]{"//email"};

    private BenchmarkRunner<JsonFilter> maxStringLengthJsonFilter;
    private BenchmarkRunner<JsonFilter> maxStringLengthMaxSizeJsonFilter;
    private BenchmarkRunner<JsonFilter> maxSizeJsonFilter;
    private BenchmarkRunner<JsonFilter> removeWhitespaceJsonFilter;
    private BenchmarkRunner<JsonFilter> anyPathJsonFilter;
    private BenchmarkRunner<JsonFilter> fullPathJsonFilter;
    private BenchmarkRunner<JsonFilter> allPathJsonFilter;

    @Param(value = {"9KB", "19KB"})
    private String fileName;

    private final boolean prettyPrinted = false;

    @Setup
    public void init() throws Exception {
        File file = new File("./src/test/resources/benchmark/shopify/" + fileName);
        int size = Integer.parseInt(fileName.substring(0, fileName.length() - 2)) * 1024 - 1;

        maxStringLengthJsonFilter = new BenchmarkRunner<>(file, true,
            new MaxStringLengthJsonFilter(maxStringLength), prettyPrinted);
        maxStringLengthMaxSizeJsonFilter = new BenchmarkRunner<>(file, true,
            new MaxStringLengthMaxSizeJsonFilter(maxStringLength, size), prettyPrinted);
        maxSizeJsonFilter = new BenchmarkRunner<>(file, true,
            new MaxSizeJsonFilter(size), prettyPrinted);
        removeWhitespaceJsonFilter = new BenchmarkRunner<>(file, true,
            new RemoveWhitespaceJsonFilter(), prettyPrinted);
        anyPathJsonFilter = new BenchmarkRunner<>(file, true,
            new AnyPathJsonFilter(size, anyPathFields, null), prettyPrinted);
        fullPathJsonFilter = new BenchmarkRunner<>(file, true,
            new FullPathJsonFilter(size, anon, null), prettyPrinted);
        allPathJsonFilter = new BenchmarkRunner<>(file, true,
            new DefaultJsonLogFilterBuilder()
                .withMaxStringLength(maxStringLength)
                .withAnonymize(anon)
                .withPrune(prune)
                .build(),
            prettyPrinted);
    }

    @Benchmark
    public long maxStringLength_core() throws IOException {
        return maxStringLengthJsonFilter.benchmarkBytes();
    }

    @Benchmark
    public long maxStringLengthMaxSize_core() throws IOException {
        return maxStringLengthMaxSizeJsonFilter.benchmarkCharacters();
    }

    @Benchmark
    public long maxSize_core() throws IOException {
        return maxSizeJsonFilter.benchmarkBytes();
    }

    @Benchmark
    public long core_remove_whitespace() throws IOException {
        return removeWhitespaceJsonFilter.benchmarkBytes();
    }

    @Benchmark
    public long anon_any_core() throws IOException {
        return anyPathJsonFilter.benchmarkBytes();
    }

    @Benchmark
    public long anon_full_core() throws IOException {
        return fullPathJsonFilter.benchmarkBytesAsArray();
    }

    @Benchmark
    public long all_core() throws IOException {
        return allPathJsonFilter.benchmarkBytes();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(ShopifyFilterBenchmark.class.getSimpleName())
            .warmupIterations(1)
            .measurementIterations(3)
            .result("target/" + System.currentTimeMillis() + ".ShopifyFilterBenchmark.json")
            .resultFormat(ResultFormatType.JSON)
            .build();
        new Runner(opt).run();
    }
}
