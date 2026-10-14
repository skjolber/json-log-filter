package com.github.skjolber.jsonfilter.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.path.matcher.DefaultJsonFilterPathMatcherFactory;
import com.github.skjolber.jsonfilter.path.properties.JsonFilterPathProperties;
import com.github.skjolber.jsonfilter.path.properties.JsonFilterProperties;
import com.github.skjolber.jsonfilter.path.properties.JsonFiltersProperties;
import com.github.skjolber.jsonfilter.path.properties.ProcessingProperties;

/**
 * Processing properties without a whitespace strategy must not drop the path filters.
 */

public class DefaultWhitespaceStrategyTest {

	private static RequestResponseJsonFilter create(ProcessingProperties processing) {
		JsonFilterProperties request = new JsonFilterProperties();
		request.setEnabled(true);
		request.setAnonymizes(List.of("$..password"));

		JsonFilterPathProperties path = new JsonFilterPathProperties();
		path.setMatcher("/api");
		path.setRequest(request);
		path.setResponse(request);

		JsonFiltersProperties properties = new JsonFiltersProperties();
		properties.setEnabled(true);
		properties.setRequests(processing);
		properties.setResponses(processing);
		properties.setPaths(List.of(path));

		return new RequestResponseJsonFilterFactory(new DefaultJsonFilterPathMatcherFactory()).requestResponseJsonFilter(properties);
	}

	@Test
	public void noWhitespaceStrategy() {
		RequestResponseJsonFilter filter = create(new ProcessingProperties());

		for(JsonFilter jsonFilter : new JsonFilter[] {
				filter.getRequestFilter("/api", true, 10), filter.getRequestFilter("/api", false, 10),
				filter.getResponseFilter("/api", true, 10), filter.getResponseFilter("/api", false, 10)}) {
			assertNotNull(jsonFilter);
			assertEquals("{\"password\":\"*\"}", jsonFilter.process("{\"password\":\"secret\"}"));
		}
	}

	@Test
	public void noWhitespaceStrategyWithMaxSize() {
		ProcessingProperties processing = new ProcessingProperties();
		processing.setMaxSize(1024);
		RequestResponseJsonFilter filter = create(processing);

		JsonFilter jsonFilter = filter.getRequestFilter("/api", true, 10);
		assertNotNull(jsonFilter);
		assertEquals("{\"password\":\"*\"}", jsonFilter.process("{\"password\":\"secret\"}"));
	}
}
