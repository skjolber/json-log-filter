package com.github.skjolber.jsonfilter.spring.autoconfigure.logbook;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.zalando.logbook.DefaultSink;
import org.zalando.logbook.HttpLogFormatter;
import org.zalando.logbook.HttpLogWriter;
import org.zalando.logbook.Sink;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;

import com.fasterxml.jackson.core.JsonFactory;
import com.github.skjolber.jsonfilter.path.RequestResponseJsonFilter;
import com.github.skjolber.jsonfilter.path.RequestResponseJsonFilterFactory;
import com.github.skjolber.jsonfilter.path.properties.ProcessingProperties;

/**
 * 
 * Use a sink so that the response can also be filtered by path.
 * 
 */

@AutoConfigureBefore(LogbookAutoConfiguration.class)
@Configuration
@EnableConfigurationProperties({ LogbookJsonFiltersProperties.class })
@ConditionalOnProperty(name = { "jsonfilter.logbook.enabled" }, havingValue = "true", matchIfMissing = true)
public class LogbookJsonFilterAutoConfiguration {

	@Bean
	public RequestResponseJsonFilter requestResponseJsonFilter(LogbookJsonFiltersProperties properties) {
		JsonFilterAntPathMatcherFactory jsonFilterAntPathMatcherFactory = new JsonFilterAntPathMatcherFactory();
		RequestResponseJsonFilterFactory c = new RequestResponseJsonFilterFactory(jsonFilterAntPathMatcherFactory);
		return c.requestResponseJsonFilter(properties);
	}

	@Bean
	@Primary
	@ConditionalOnBean(Sink.class)
	public Sink jsonFilterSinkWrapper(Sink sink, RequestResponseJsonFilter filter, LogbookJsonFiltersProperties properties) {
		JsonFactory jsonFactory = new JsonFactory();
		ProcessingProperties requests = properties.getRequests();
		ProcessingProperties responses = properties.getResponses();
		return new PathFilterSink(sink, filter, requests.isValidate(), responses.isValidate(), requests.isCompact(), responses.isCompact(), jsonFactory);
	}

	@ConditionalOnMissingBean(Sink.class)
    @Bean
    public Sink defaultSink(
    		RequestResponseJsonFilter filter,
    		HttpLogFormatter formatter,
            HttpLogWriter writer, LogbookJsonFiltersProperties properties) {
    	Sink sink = new DefaultSink(formatter, writer);
		JsonFactory jsonFactory = new JsonFactory();
		ProcessingProperties requests = properties.getRequests();
		ProcessingProperties responses = properties.getResponses();
		return new PathFilterSink(sink, filter, requests.isValidate(), responses.isValidate(), requests.isCompact(), responses.isCompact(), jsonFactory);
    }
	
}
