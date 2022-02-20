package com.github.skjolber.jsonfilter.spring.logbook;

import org.springframework.lang.Nullable;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.handler.DispatcherServletWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.Logbook.ResponseProcessingStage;

public class LogbookServletInvocableHandlerMethod extends ServletInvocableHandlerMethod {
	
	protected final Logbook logbook;

	public LogbookServletInvocableHandlerMethod(HandlerMethod handlerMethod, Logbook logbook) {
		super(handlerMethod);
		this.logbook = logbook;
	}

	@Override
	public void invokeAndHandle(ServletWebRequest webRequest, ModelAndViewContainer mavContainer,
			Object... providedArgs) throws Exception {
		super.invokeAndHandle(webRequest, mavContainer, providedArgs);
	}

	@Nullable
	public Object invokeForRequest(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer, Object... providedArgs) throws Exception {

		Object nativeRequest = request.getNativeRequest();
		if(nativeRequest instanceof PreprocessedRemoteRequest) {
			final PreprocessedRemoteRequest remoteRequest = (PreprocessedRemoteRequest) nativeRequest;
			if(!remoteRequest.isDatabindingPerformed()) {
				try {
					Object[] args = getMethodArgumentValues(new DispatcherServletWebRequest(remoteRequest), mavContainer, providedArgs);
	
					// detect whether databinding was performed
					// typically this will not be the case for streaming approaches
					boolean streaming = args.length == 1 && args[0] instanceof PreprocessedRemoteRequest;

					remoteRequest.setDatabindingPerformed(true);
					remoteRequest.setDatabindingSuccessful(!streaming);
					
					// if not streaming, we can assume valid JSON, so
					// we can use the fastest filtering and in addition 
					// can append the raw content to JSON logger after compacting
					ResponseProcessingStage processing = logbook.process(remoteRequest).write();
					remoteRequest.setAttribute(LogbookWebMvcRegistrations.responseProcessingStageName, processing);
					
					return doInvoke(args);
				} catch(Exception e) {
					// Databinding failed, but
					// might be valid JSON still 
					//
					// slow path
					remoteRequest.setDatabindingPerformed(true);
					remoteRequest.setDatabindingSuccessful(false);
					
					ResponseProcessingStage processing = logbook.process(remoteRequest).write();
					remoteRequest.setAttribute(LogbookWebMvcRegistrations.responseProcessingStageName, processing);
	
					throw e;
				}
			}
		} 
		return doInvoke(getMethodArgumentValues(request, mavContainer, providedArgs));
	}

}
