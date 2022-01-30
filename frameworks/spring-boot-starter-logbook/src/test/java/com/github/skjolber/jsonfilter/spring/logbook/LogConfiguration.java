package com.github.skjolber.jsonfilter.spring.logbook;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.zalando.logbook.HttpLogFormatter;
import org.zalando.logbook.Sink;
import org.zalando.logbook.json.FastJsonHttpLogFormatter;
import org.zalando.logbook.logstash.LogstashLogbackSink;

@Configuration
@Profile("json")
public class LogConfiguration {

	@Bean
	public FastJsonHttpLogFormatter formatter() {
		return new FastJsonHttpLogFormatter();
	}
	
	@Bean
	public Sink configure(HttpLogFormatter f) {
		return new LogstashLogbackSink(f);
	}
	
}
