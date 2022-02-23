package com.github.skjolber.jsonfilter.spring.logbook;

import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

public class DefaultMethodArgumentValuesDetector implements MethodArgumentValuesDetector {

	@Override
	public boolean isDatabindingArguments(NativeWebRequest request, ModelAndViewContainer mavContainer, Object[] args) {
		
		for(Object arg : args) {
			if(arg instanceof PreprocessedRemoteRequest) {
				return false;
			}
		}
		
		return true;
	}

}
