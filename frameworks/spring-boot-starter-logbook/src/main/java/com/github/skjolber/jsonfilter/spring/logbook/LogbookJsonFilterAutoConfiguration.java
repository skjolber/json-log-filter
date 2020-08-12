package com.github.skjolber.jsonfilter.spring.logbook;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.zalando.logbook.DefaultSink;
import org.zalando.logbook.HttpLogFormatter;
import org.zalando.logbook.HttpLogWriter;
import org.zalando.logbook.Sink;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;

import com.github.skjolber.jsonfilter.spring.JsonFilterAutoConfiguration;
import com.github.skjolber.jsonfilter.spring.RequestResponseJsonFilter;

/**
 * 
 * Use a sink so that the response can also be filtered by path.
 * 
 */


@AutoConfigureAfter(JsonFilterAutoConfiguration.class)
@AutoConfigureBefore(LogbookAutoConfiguration.class)
@Configuration
@ConditionalOnProperty(name = { "jsonfilter.enabled" }, havingValue = "true", matchIfMissing = true)
public class LogbookJsonFilterAutoConfiguration {

	@Configuration
	@ConditionalOnBean(Sink.class)
	public static class SinkWrapper {

		@Bean
		@Primary
		public Sink jsonFilterSinkWrapper(Sink sink, RequestResponseJsonFilter filter) {
			return new PathFilterSink(sink, filter);
		}
	}

	@Configuration
	@ConditionalOnMissingBean(Sink.class)
	public static class Default {

	    @Bean
	    public Sink defaultSink(
	    		RequestResponseJsonFilter filter,
	    		HttpLogFormatter formatter,
	            HttpLogWriter writer) {
	    	Sink sink = new DefaultSink(formatter, writer);
			return new PathFilterSink(sink, filter);
	    }

	}
	
	
}
