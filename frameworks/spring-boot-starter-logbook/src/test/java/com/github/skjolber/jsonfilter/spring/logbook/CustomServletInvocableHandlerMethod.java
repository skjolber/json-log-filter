package com.github.skjolber.jsonfilter.spring.logbook;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.handler.DispatcherServletWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.Logbook.RequestWritingStage;
import org.zalando.logbook.Logbook.ResponseProcessingStage;

import com.github.skjolberg.jsonfilter.spring.logbook.servlet.LogbookFilter;
import com.github.skjolberg.jsonfilter.spring.logbook.servlet.RemoteRequest;

public class CustomServletInvocableHandlerMethod extends ServletInvocableHandlerMethod {

	
	private final Logbook logbook;
	
	public CustomServletInvocableHandlerMethod(HandlerMethod handlerMethod, Logbook logbook) {
		super(handlerMethod);
		
		this.logbook = logbook;
	}

	@Override
	public void invokeAndHandle(ServletWebRequest webRequest, ModelAndViewContainer mavContainer,
			Object... providedArgs) throws Exception {
		super.invokeAndHandle(webRequest, mavContainer, providedArgs);
	}
	
	/**
	 * Invoke the method after resolving its argument values in the context of the given request.
	 * <p>Argument values are commonly resolved through
	 * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}.
	 * The {@code providedArgs} parameter however may supply argument values to be used directly,
	 * i.e. without argument resolution. Examples of provided argument values include a
	 * {@link WebDataBinder}, a {@link SessionStatus}, or a thrown exception instance.
	 * Provided argument values are checked before argument resolvers.
	 * <p>Delegates to {@link #getMethodArgumentValues} and calls {@link #doInvoke} with the
	 * resolved arguments.
	 * @param request the current request
	 * @param mavContainer the ModelAndViewContainer for this request
	 * @param providedArgs "given" arguments matched by type, not resolved
	 * @return the raw value returned by the invoked method
	 * @throws Exception raised if no suitable argument resolver can be found,
	 * or if the method raised an exception
	 * @see #getMethodArgumentValues
	 * @see #doInvoke
	 */
	@Nullable
	public Object invokeForRequest(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer,
			Object... providedArgs) throws Exception {

		try {

			System.out.println("Request is " + request);
	
			Object nativeRequest = request.getNativeRequest();
			if(nativeRequest instanceof HttpServletRequest) {
				System.out.println("Native request is " + request.getNativeRequest());
		
				HttpServletRequest nativeHttpServletRequest = (HttpServletRequest)nativeRequest;
				
				final RemoteRequest remoteRequest = (RemoteRequest) new RemoteRequest(nativeHttpServletRequest).withBody();
				
		
				Object[] args = null;
				try {
					args = getMethodArgumentValues(new DispatcherServletWebRequest(remoteRequest), mavContainer, providedArgs);
					System.out.println("OK " + request.getNativeRequest().getClass());
	
					remoteRequest.setAttribute("validated", "true");
					ResponseProcessingStage processing = logbook.process(remoteRequest).write();
					remoteRequest.setAttribute(LogbookFilter.responseProcessingStageName, processing);
	
					System.out.println(Arrays.asList(args));
				} catch(Exception e) {
					System.out.println("FAIL " + request.getNativeRequest().getClass());
					remoteRequest.setAttribute("validated", "false");
					
					ResponseProcessingStage processing = logbook.process(remoteRequest).write();
					remoteRequest.setAttribute(LogbookFilter.responseProcessingStageName, processing);
	
					e.printStackTrace();
					throw e;
				}
				return doInvoke(args);
			} 
			Object[] args = getMethodArgumentValues(request, mavContainer, providedArgs);
			return doInvoke(args);
			
		} catch(Exception e) {
			System.out.println("whoopps " + request.getNativeRequest().getClass());
			e.printStackTrace();
			throw e;
		}
	}
	
	@Override
	protected Object[] getMethodArgumentValues(NativeWebRequest request, ModelAndViewContainer mavContainer,
			Object... providedArgs) throws Exception {
		return super.getMethodArgumentValues(request, mavContainer, providedArgs);
	}
	
}
