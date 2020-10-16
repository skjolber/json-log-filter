package com.github.skjolber.jsonfilter.base;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import static com.github.skjolber.jsonfilter.base.AbstractJsonFilter.*;

public class SkipSubtreeJsonFilterTest  extends DefaultJsonFilterTest {

	public SkipSubtreeJsonFilterTest() throws Exception {
		super(true, 1);
	}

	@Test
	public void passthrough_success() throws Exception {
		SkipSubtreeJsonFilter filter = new SkipSubtreeJsonFilter(-1, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
		assertThat(filter).hasPassthrough();
	}

}
