package com.github.skjolber.jsonfilter.spring.logbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.HttpLogFormatter;
import org.zalando.logbook.HttpLogWriter;
import org.zalando.logbook.Sink;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;

import com.github.skjolber.jsonfilter.spring.JsonFilterAutoConfiguration;
import com.github.skjolber.jsonfilter.spring.RequestResponseJsonFilter;

@AutoConfigureAfter(JsonFilterAutoConfiguration.class)
@AutoConfigureBefore(LogbookAutoConfiguration.class)
@Configuration
public class LogbookJsonFilterAutoConfiguration {

	private static Logger log = LoggerFactory.getLogger(LogbookJsonFilterAutoConfiguration.class);

	/**
	 * Use a sink so that the response can also be filtered by path.
	 * 
	 * @param formatter target formatter
	 * @param writer target writer
	 * @param filter target filters
	 * @return a newly created sink which also filters.
	 */
	
	@Bean
	@ConditionalOnMissingBean(Sink.class)
	@ConditionalOnBean(RequestResponseJsonFilter.class)
	public Sink sink(HttpLogFormatter formatter, HttpLogWriter writer, RequestResponseJsonFilter filter) {
		log.info("Add per-path JSON body filter");
		return new PathFilterSink(formatter, writer, filter);
	}
	

	
}
