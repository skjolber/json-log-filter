module com.github.skjolber.jsonfilter.simdjson {
	exports com.github.skjolber.jsonfilter.simdjson;

	requires tools.jackson.core;
	requires org.apache.commons.io;
	requires json.log.filter.api;
	requires json.log.filter.base;

	provides com.github.skjolber.jsonfilter.JsonFilterFactory with
	com.github.skjolber.jsonfilter.simdjson.SimdJsonJsonFilterFactory;
}
