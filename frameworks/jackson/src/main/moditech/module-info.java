module com.github.skjolber.jsonfilter.jackson {
	exports com.github.skjolber.jsonfilter.jackson;

	requires com.fasterxml.jackson.core;
	requires json.log.filter.api;
	requires json.log.filter.base;
	requires org.apache.commons.io;

	provides com.github.skjolber.jsonfilter.JsonFilterFactory with
	com.github.skjolber.jsonfilter.jackson.JacksonJsonFilterFactory;
}