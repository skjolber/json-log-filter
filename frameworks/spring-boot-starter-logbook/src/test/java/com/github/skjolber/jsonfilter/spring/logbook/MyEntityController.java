package com.github.skjolber.jsonfilter.spring.logbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MyEntityController {

    private static Logger log = LoggerFactory.getLogger(MyEntityController.class);

    private final AtomicLong counter = new AtomicLong();

    @GetMapping("/get")
    public MyEntity unprotected() {
        log.info("GET");

        return new MyEntity(counter.incrementAndGet(), "Hello get", "Magnus");
    }

    @PostMapping(path = "/post", consumes = "application/json", produces = "application/json")
    public MyEntity unprotectedPost(@RequestBody MyEntity greeting) {
        log.info("POST");
        return new MyEntity(counter.incrementAndGet(), "Hello post", "Thomas");
    }


    @PostMapping(path = "/postStreaming", consumes = "application/json", produces = "application/json")
    public MyEntity unprotectedPost(HttpServletRequest request) throws IOException {
    	ServletInputStream inputStream = request.getInputStream();
    	IOUtils.toByteArray(inputStream);
        log.info("POST");
        return new MyEntity(counter.incrementAndGet(), "Hello post", "Thomas");
    }
}