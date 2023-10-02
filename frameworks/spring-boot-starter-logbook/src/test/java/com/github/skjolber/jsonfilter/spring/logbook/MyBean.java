package com.github.skjolber.jsonfilter.spring.logbook;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

@Component
public class MyBean {

	@Autowired
	private RequestMappingHandlerAdapter adapter;
	
	@PostConstruct
	public void prioritizeCustomArgumentMethodHandlers () {
	    List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>(adapter.getArgumentResolvers());
	    List<HandlerMethodArgumentResolver> customResolvers = adapter.getCustomArgumentResolvers();
	    argumentResolvers.removeAll(customResolvers);
	    argumentResolvers.addAll (0, customResolvers);
	    adapter.setArgumentResolvers (argumentResolvers);
	}
	
}
