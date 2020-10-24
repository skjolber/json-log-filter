set -e
mvn clean install

/usr/lib/jvm/java-1.8.0-openjdk/jre/bin/java -jar target/benchmarks.jar -rff results.jdk8.json -rf json CveFilterBenchmark

/usr/lib/jvm/java-11-openjdk/bin/java -jar target/benchmarks.jar -rff results.jdk11.json  -rf json CveFilterBenchmark
