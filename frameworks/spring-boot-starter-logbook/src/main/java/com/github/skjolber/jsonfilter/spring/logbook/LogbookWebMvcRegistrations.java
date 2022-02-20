package com.github.skjolber.jsonfilter.spring.logbook;

import java.util.UUID;

import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.Logbook.ResponseProcessingStage;

public class LogbookWebMvcRegistrations implements WebMvcRegistrations {

    public static final String responseProcessingStageName = ResponseProcessingStage.class.getName() + "-" + UUID.randomUUID();

	protected final Logbook logbook;

	public LogbookWebMvcRegistrations(Logbook logbook) {
		this.logbook = logbook;
	}
	
	@Override
	public RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
		return new LogbookRequestMappingHandlerAdapter(logbook);
	}
}