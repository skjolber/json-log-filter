package com.github.skjolber.jsonfilter.test;

import com.github.skjolber.jsonfilter.JsonFilter;

/**
 * 
 * Abstract test class.
 *
 */

public abstract class AbstractJsonFilterTest {

	protected JsonFilterRunner runner;
	
	public AbstractJsonFilterTest(JsonFilterRunner runner) {
		this.runner = runner;
	}
	
	protected JsonFilterResultSubject assertThat(JsonFilter filter) throws Exception {
		JsonFilterResult process = runner.process(filter);
			
		return JsonFilterResultSubject.assertThat(process);
	}

}
