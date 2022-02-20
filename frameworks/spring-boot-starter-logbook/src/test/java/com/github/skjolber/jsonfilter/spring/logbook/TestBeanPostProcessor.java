package com.github.skjolber.jsonfilter.spring.logbook;

import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

@Configuration
public class TestBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object o, String s) throws BeansException {
        return o;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (beanName.equalsIgnoreCase("requestMappingHandlerAdapter")) {
        	/*
            RequestMappingHandlerAdapter requestMappingHandlerAdapter = (RequestMappingHandlerAdapter) bean;
            
            List<HandlerMethodArgumentResolver> argumentResolvers = requestMappingHandlerAdapter.getArgumentResolvers();
            List<HandlerMethodArgumentResolver> modifiedArgumentResolvers = new ArrayList<>(argumentResolvers.size());                
            for(int i =1; i< argumentResolvers.size();i++){
                modifiedArgumentResolvers.add(argumentResolvers.get(i));
            }
            modifiedArgumentResolvers.add(new TestRequestBodyMethodProcessor(requestMappingHandlerAdapter.getMessageConverters(), new ArrayList<Object>()));
            ((RequestMappingHandlerAdapter) bean).setArgumentResolvers(null);
            ((RequestMappingHandlerAdapter) bean).setArgumentResolvers(modifiedArgumentResolvers);
            */
        }
        return bean;
    }
}