module com.github.skjolber.jsonfilter.core {
	requires com.github.skjolber.jsonfilter;
	requires com.github.skjolber.jsonfilter.base;
	requires com.github.skjolber.jsonfilter.base.path;
	
	exports com.github.skjolber.jsonfilter.core;
	exports com.github.skjolber.jsonfilter.core.pp;
	exports com.github.skjolber.jsonfilter.core.ws;
	
	provides com.github.skjolber.jsonfilter.JsonFilterFactory with com.github.skjolber.jsonfilter.core.DefaultJsonFilterFactory;
	
}