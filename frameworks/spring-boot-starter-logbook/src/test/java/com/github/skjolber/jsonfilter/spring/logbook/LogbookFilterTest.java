package com.github.skjolber.jsonfilter.spring.logbook;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zalando.logbook.Sink;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class LogbookFilterTest {
	
	@Autowired
	private Sink sink;

	@Test 
	public void testSpringBootStarter() {
		assertTrue(sink instanceof PathFilterSink);
	}

}