package com.github.skjolber.jsonfilter.spring.logbook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.zalando.logbook.Logbook;

@Component
public class CustomMvcRegistrations implements WebMvcRegistrations {

	private final Logbook logbook;

	@Autowired
	public CustomMvcRegistrations(Logbook logbook) {
		this.logbook = logbook;
	}
	
	@Override
	public RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
		return new CustomRequestMappingHandlerAdapter(logbook);
	}
}