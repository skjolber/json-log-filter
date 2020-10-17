package com.github.skjolber.jsonfilter.jackson;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class JacksonSinglePathMaxStringLengthJsonFilterTest extends AbstractJacksonJsonFilterTest {

	public JacksonSinglePathMaxStringLengthJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new JacksonSinglePathMaxStringLengthJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.ANON)).hasPassthrough();
	}
	
	@Test
	public void anonymize() throws Exception {
		assertThat(new JacksonSinglePathMaxStringLengthJsonFilter(-1, DEFAULT_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_PATH);
		assertThat(new JacksonSinglePathMaxStringLengthJsonFilter(-1, DEEP_PATH1, FilterType.ANON)).hasAnonymized(DEEP_PATH1);
	}

	@Test
	public void anonymizeWildcard() throws Exception {
		assertThat(new JacksonSinglePathMaxStringLengthJsonFilter(-1, DEFAULT_WILDCARD_PATH, FilterType.ANON)).hasAnonymized(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void prune() throws Exception {
		assertThat(new JacksonSinglePathMaxStringLengthJsonFilter(-1, DEFAULT_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_PATH);
		assertThat(new JacksonSinglePathMaxStringLengthJsonFilter(-1, DEEP_PATH3, FilterType.PRUNE)).hasPruned(DEEP_PATH3);
	}

	@Test
	public void pruneWildcard() throws Exception {
		assertThat(new JacksonSinglePathMaxStringLengthJsonFilter(-1, DEFAULT_WILDCARD_PATH, FilterType.PRUNE)).hasPruned(DEFAULT_WILDCARD_PATH);
	}

	@Test
	public void maxStringLength() throws Exception {
		assertThat(new JacksonSinglePathMaxStringLengthJsonFilter(DEFAULT_MAX_LENGTH, PASSTHROUGH_XPATH, FilterType.PRUNE)).hasMaxStringLength(DEFAULT_MAX_LENGTH);
	}
	
	@Test
	public void testConvenienceMethods() throws IOException {
		testConvenienceMethods(new JacksonSinglePathMaxStringLengthJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.ANON));
	}	
	
	/*
	@Test
	public void maxStringLengthAnonymize() throws Exception {
		assertThat(new JacksonSinglePathMaxStringLengthJsonFilter(DEFAULT_MAX_LENGTH, DEFAULT_XPATH, FilterType.ANON))
			.hasMaxStringLength(DEFAULT_MAX_LENGTH)
			.hasAnonymized(DEFAULT_XPATH);
	}

	@Test
	public void maxStringLengthPrune() throws Exception {
		assertThat(new JacksonSinglePathMaxStringLengthJsonFilter(DEFAULT_MAX_LENGTH, DEFAULT_XPATH, FilterType.PRUNE))
			.hasMaxStringLength(DEFAULT_MAX_LENGTH)
			.hasPruned(DEFAULT_XPATH);
	}
*/
}
