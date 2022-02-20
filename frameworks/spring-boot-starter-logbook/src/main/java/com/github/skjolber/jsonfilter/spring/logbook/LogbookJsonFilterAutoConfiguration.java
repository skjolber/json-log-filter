package com.github.skjolber.jsonfilter.spring.logbook;

import static javax.servlet.DispatcherType.ASYNC;
import static javax.servlet.DispatcherType.REQUEST;
import static org.apiguardian.api.API.Status.INTERNAL;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.apiguardian.api.API;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.zalando.logbook.DefaultSink;
import org.zalando.logbook.HttpLogFormatter;
import org.zalando.logbook.HttpLogWriter;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.Sink;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;
import org.zalando.logbook.autoconfigure.LogbookProperties;

import com.fasterxml.jackson.core.JsonFactory;
import com.github.skjolber.jsonfilter.path.RequestResponseJsonFilter;
import com.github.skjolber.jsonfilter.path.RequestResponseJsonFilterFactory;
import com.github.skjolberg.jsonfilter.spring.logbook.servlet.LogbookFilter;

/**
 * 
 * Use a sink so that the response can also be filtered by path.
 * 
 */

@AutoConfigureBefore(LogbookAutoConfiguration.class)
@Configuration
@EnableConfigurationProperties({ LogbookJsonFiltersProperties.class })
@ConditionalOnProperty(name = { "jsonfilter.logbook.enabled" }, havingValue = "true", matchIfMissing = true)
public class LogbookJsonFilterAutoConfiguration {

	@Bean
	public RequestResponseJsonFilter requestResponseJsonFilter(LogbookJsonFiltersProperties properties) {
		JsonFilterAntPathMatcherFactory jsonFilterAntPathMatcherFactory = new JsonFilterAntPathMatcherFactory();
		RequestResponseJsonFilterFactory c = new RequestResponseJsonFilterFactory(jsonFilterAntPathMatcherFactory, properties.isValidateRequests(), properties.isValidateResponses());
		return c.requestResponseJsonFilter(properties);
	}
	
	@Bean
	@Primary
	@ConditionalOnBean(Sink.class)
	public Sink jsonFilterSinkWrapper(Sink sink, RequestResponseJsonFilter filter, LogbookJsonFiltersProperties properties) {
		JsonFactory jsonFactory = new JsonFactory();
		return new PathFilterSink(sink, filter, properties.isValidateRequests(), properties.isValidateResponses(), properties.isCompactRequests(), properties.isCompactResponses(), jsonFactory);
	}

	@ConditionalOnMissingBean(Sink.class)
    @Bean
    public Sink defaultSink(
    		RequestResponseJsonFilter filter,
    		HttpLogFormatter formatter,
            HttpLogWriter writer, LogbookJsonFiltersProperties properties) {
    	Sink sink = new DefaultSink(formatter, writer);
		JsonFactory jsonFactory = new JsonFactory();
		return new PathFilterSink(sink, filter, properties.isValidateRequests(), properties.isValidateResponses(), properties.isCompactRequests(), properties.isCompactResponses(), jsonFactory);
    }
	
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = Type.SERVLET)
    @ConditionalOnClass({
            Servlet.class,
            LogbookFilter.class
    })
    static class ServletFilterConfiguration {

        private static final String FILTER_NAME = "logbookFilter";

        private final LogbookProperties properties;

        @API(status = INTERNAL)
        @Autowired
        public ServletFilterConfiguration(final LogbookProperties properties) {
            this.properties = properties;
        }

        @Bean
        public FilterRegistrationBean logbookFilter(final Logbook logbook) {
            final LogbookFilter filter = new LogbookFilter(logbook);
                    
            return newFilter(filter, FILTER_NAME, Ordered.LOWEST_PRECEDENCE);
        }

        static FilterRegistrationBean newFilter(final Filter filter, final String filterName, final int order) {
            @SuppressWarnings("unchecked") // as of Spring Boot 2.x
            final FilterRegistrationBean registration = new FilterRegistrationBean(filter);
            registration.setName(filterName);
            registration.setDispatcherTypes(REQUEST, ASYNC);
            registration.setOrder(order);
            return registration;
        }

    }
}
