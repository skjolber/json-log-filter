package com.github.skjolber.jsonfilter.spring.logbook;

import java.util.concurrent.atomic.AtomicLong;

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
        System.out.println("TEST");
        return new MyEntity(counter.incrementAndGet(), "Hello post", "Thomas");
    }

}