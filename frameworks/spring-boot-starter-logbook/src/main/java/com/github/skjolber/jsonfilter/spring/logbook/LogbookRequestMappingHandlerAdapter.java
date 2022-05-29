package com.github.skjolber.jsonfilter.spring.logbook;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.Logbook.ResponseProcessingStage;
import org.zalando.logbook.Logbook.ResponseWritingStage;

import com.github.skjolber.jsonfilter.spring.autoconfigure.logbook.PathFilterSink;
import com.github.skjolber.jsonfilter.spring.logbook.servlet.LocalResponse;

public class LogbookRequestMappingHandlerAdapter extends RequestMappingHandlerAdapter {

	protected final Logbook logbook;
	protected final MethodArgumentValuesDetector detector;

	public LogbookRequestMappingHandlerAdapter(Logbook logbook, MethodArgumentValuesDetector detector) {
		this.logbook = logbook;
		this.detector = detector;
	}

	@Override
	protected ServletInvocableHandlerMethod createInvocableHandlerMethod(HandlerMethod handlerMethod) {
		return new LogbookServletInvocableHandlerMethod(handlerMethod, logbook, detector);
	}

	@Override
	protected ModelAndView invokeHandlerMethod(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
		ResponseProcessingStage previousProcessing = (ResponseProcessingStage) request.getAttribute(LogbookWebMvcRegistrations.responseProcessingStageName);
		if(previousProcessing != null) {
			// log response only, request already logged
			LocalResponse localResponse = new LocalResponse(response, request.getProtocol()).withBody();
			ModelAndView invokeHandlerMethod = super.invokeHandlerMethod(request, localResponse, handlerMethod);

			ResponseWritingStage writing = previousProcessing.process(localResponse);
			response.flushBuffer();
			writing.write();

			return invokeHandlerMethod;
		} else {
			// log request and response
			LocalResponse localResponse = new LocalResponse(response, request.getProtocol()).withBody();
			PreprocessedRemoteRequest remoteRequest = new PreprocessedRemoteRequest(request).withBody();
			ModelAndView invokeHandlerMethod = super.invokeHandlerMethod(remoteRequest, localResponse, handlerMethod);
			ResponseProcessingStage processing = (ResponseProcessingStage) remoteRequest.getAttribute(LogbookWebMvcRegistrations.responseProcessingStageName);

			ResponseWritingStage writing = processing.process(localResponse);
			response.flushBuffer();
			writing.write();

			return invokeHandlerMethod;
		}
	}

}
