package com.github.skjolber.jsonfilter.core;

import static com.github.skjolber.jsonfilter.base.AbstractJsonFilter.FILTER_ANONYMIZE_JSON;
import static com.github.skjolber.jsonfilter.base.AbstractJsonFilter.FILTER_PRUNE_MESSAGE_JSON;
import static com.github.skjolber.jsonfilter.base.AbstractJsonFilter.FILTER_TRUNCATE_MESSAGE;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class SkipObjectJsonFilterTest extends DefaultJsonFilterTest {

	public SkipObjectJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		try {
			SkipObjectJsonFilter filter = new SkipObjectJsonFilter(-1, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
			assertThat(filter).hasPassthrough();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
