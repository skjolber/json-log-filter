package com.github.skjolberg.jsonfilter.spring.logbook.servlet;
import java.io.IOException;
import javax.servlet.AsyncEvent;

@FunctionalInterface
interface AsyncOnCompleteListener {
    void onComplete(AsyncEvent event) throws IOException;
}