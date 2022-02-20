package com.github.skjolber.jsonfilter.spring.logbook;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;

@AutoConfigureAfter(LogbookAutoConfiguration.class)
@ConditionalOnProperty(name = { "jsonfilter.logbook.enabled" }, havingValue = "true", matchIfMissing = true)
public class LogbookRestAutoConfiguration {

	@Bean
	public LogbookWebMvcRegistrations CustomMvcRegistrations(Logbook logbook) {
		return new LogbookWebMvcRegistrations(logbook);
	}

}
