package com.github.skjolber.jsonfilter.spring.logbook;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zalando.logbook.Sink;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("json")
public class LogbookJsonSinkTest {
	
    @LocalServerPort
    private int randomServerPort;

    @Autowired
    private TestRestTemplate restTemplate;

	@Autowired
	private Sink sink;

	@Test 
	public void testSpringBootStarterGet() {
		assertTrue(sink instanceof PathFilterSink);
		
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<String>(headers);

        String url = "http://localhost:" + randomServerPort + "/api/get";

        ResponseEntity<MyEntity> response = restTemplate.exchange(url, HttpMethod.GET, entity, MyEntity.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());

        assertThat(response.getBody().getContent()).isEqualTo("Hello get");
	}
	
	@Test 
	public void testSpringBootStarterPost() {
		assertTrue(sink instanceof PathFilterSink);

        MyEntity e = new MyEntity();
        e.setContent("request-content");
        e.setName("request-name");

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(e, headers);
        
        String url = "http://localhost:" + randomServerPort + "/api/post";

        ResponseEntity<MyEntity> response = restTemplate.exchange(url, HttpMethod.POST, entity, MyEntity.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());

        assertThat(response.getBody().getContent()).isEqualTo("Hello post");
	}

	@Test 
	public void testSpringBootStarterPostInvalidJson() {
		assertTrue(sink instanceof PathFilterSink);

		String invalid = "{name=this\n\n\n\\//\"}";
		
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(invalid, headers);
        
        String url = "http://localhost:" + randomServerPort + "/api/post";

        ResponseEntity<MyEntity> response = restTemplate.exchange(url, HttpMethod.POST, entity, MyEntity.class);
        assertTrue(response.getStatusCode().is4xxClientError());
	}

}