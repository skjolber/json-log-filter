module com.github.skjolber.jsonfilter.core {
	requires com.github.skjolber.jsonfilter;
	requires com.github.skjolber.jsonfilter.base;
	
    exports com.github.skjolber.jsonfilter.core;
    
	provides com.github.skjolber.jsonfilter.JsonFilterFactory with
	com.github.skjolber.jsonfilter.core.JsonFilterFactory;
    
}