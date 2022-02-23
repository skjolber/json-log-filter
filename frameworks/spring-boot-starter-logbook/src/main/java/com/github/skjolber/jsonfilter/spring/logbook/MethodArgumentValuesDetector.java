package com.github.skjolber.jsonfilter.spring.logbook;

import org.springframework.lang.Nullable;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

public interface MethodArgumentValuesDetector {

	boolean isDatabindingArguments(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer, Object[] args);
	
}
