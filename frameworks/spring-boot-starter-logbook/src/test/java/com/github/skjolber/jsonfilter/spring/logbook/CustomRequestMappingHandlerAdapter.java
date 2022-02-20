package com.github.skjolber.jsonfilter.spring.logbook;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;
import org.zalando.logbook.Logbook;

public class CustomRequestMappingHandlerAdapter extends RequestMappingHandlerAdapter {

	private Logbook logbook;

	public CustomRequestMappingHandlerAdapter(Logbook logbook) {
		this.logbook = logbook;
	}

	@Override
	protected ServletInvocableHandlerMethod createInvocableHandlerMethod(HandlerMethod handlerMethod) {
		return new CustomServletInvocableHandlerMethod(handlerMethod, logbook);
	}

	@Override
	protected ModelAndView invokeHandlerMethod(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
		return super.invokeHandlerMethod(request, response, handlerMethod);
	}

}
