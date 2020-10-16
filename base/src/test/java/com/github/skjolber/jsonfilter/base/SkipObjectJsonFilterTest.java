package com.github.skjolber.jsonfilter.base;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import static com.github.skjolber.jsonfilter.base.AbstractJsonFilter.*;

public class SkipObjectJsonFilterTest  extends DefaultJsonFilterTest {

	public SkipObjectJsonFilterTest() throws Exception {
		super(true, 1);
	}

	@Test
	public void passthrough_success() throws Exception {
		SkipObjectJsonFilter filter = new SkipObjectJsonFilter(-1, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
		assertThat(filter).hasPassthrough();
	}

}
