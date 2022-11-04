package com.github.skjolber.jsonfilter.core;

import static com.github.skjolber.jsonfilter.base.AbstractJsonFilter.FILTER_ANONYMIZE_JSON;
import static com.github.skjolber.jsonfilter.base.AbstractJsonFilter.FILTER_PRUNE_MESSAGE_JSON;
import static com.github.skjolber.jsonfilter.base.AbstractJsonFilter.FILTER_TRUNCATE_MESSAGE;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class SkipObjectJsonFilterTest extends DefaultJsonFilterTest {

	public SkipObjectJsonFilterTest() throws Exception {
		super(true, 1, true);
	}

	@Test
	public void passthrough_success() throws Exception {
		SkipObjectJsonFilter filter = new SkipObjectJsonFilter(-1, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
		assertThat(filter).hasPassthrough();
	}

}
