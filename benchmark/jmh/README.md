# JMH benchmarks

[JMH] is a modern benchmarking framework. This project implements a few test benches for the JSON filters included in this Git repository.

# Usage
Run java command line (from project root)

```
java -jar target/benchmarks.jar <benchmark name or empty> -rf json 
```

e.g. from the project root directory

```
mvn clean install && java -jar target/benchmarks.jar -rf json 
```

for all benchmarks. Display the file `jmh-result` with a visualizer like [JMH Visualizer].

# Benchmark results

## 1.0.0:

### Platform:

 * AMD Ryzen 3700, 
 * Fedora Linux 5.6.7-300.

### Results

 * [openjdk-1.8.0.252.b09-0](https://jmh.morethan.io/?source=https://raw.githubusercontent.com/skjolber/json-log-filter/master/benchmark/jmh/results/jmh-results-1.0.0.jdk8.json&topBar=off)
 * [openjdk-11.0.6.10-0](https://jmh.morethan.io/?source=https://raw.githubusercontent.com/skjolber/json-log-filter/master/benchmark/jmh/results/jmh-results-1.0.0.jdk11.json&topBar=off)

[JMH]: 		http://openjdk.java.net/projects/code-tools/jmh/
[JMH Visualizer]:	http://jmh.morethan.io/

[visualization]:	https://jmh.morethan.io/?source=https://raw.githubusercontent.com/skjolber/json-log-filter/master/docs/benchmark/jmh-result.json&topBar=off
|
