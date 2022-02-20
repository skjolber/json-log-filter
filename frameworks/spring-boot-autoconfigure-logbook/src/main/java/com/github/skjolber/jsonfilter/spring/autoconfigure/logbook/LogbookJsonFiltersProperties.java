package com.github.skjolber.jsonfilter.spring.autoconfigure.logbook;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.github.skjolber.jsonfilter.path.properties.JsonFiltersProperties;

@ConfigurationProperties(prefix = "jsonfilter.logbook")
public class LogbookJsonFiltersProperties extends JsonFiltersProperties {

}
