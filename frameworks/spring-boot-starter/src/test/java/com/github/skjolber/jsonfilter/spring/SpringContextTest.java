package com.github.skjolber.jsonfilter.spring;
import static org.junit.Assert.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.skjolber.jsonfilter.JsonFilter;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class SpringContextTest {
	
	@Autowired
	private RequestResponseJsonFilter requestResponseJsonFilter;

	@Test 
	public void testRequestFilter() {
		JsonFilter requestFilter = requestResponseJsonFilter.getRequestFilter("/abc");
		assertNotNull(requestFilter);
	}

}