package com.github.skjolber.jsonfilter.jackson;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class JacksonMultiAnyPathMaxStringLengthJsonFilterTest extends AbstractJacksonJsonFilterTest {

	public JacksonMultiAnyPathMaxStringLengthJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new JacksonMultiAnyPathMaxStringLengthJsonFilter(-1, null, null)).hasPassthrough();
		assertThat(new JacksonMultiAnyPathMaxStringLengthJsonFilter(-1, new String[]{ANY_PASSTHROUGH_XPATH}, new String[]{ANY_PASSTHROUGH_XPATH})).hasPassthrough();
	}
	
	@Test
	public void anonymizeAny() throws Exception {
		assertThat(new JacksonMultiAnyPathMaxStringLengthJsonFilter(-1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH);
	}

	@Test
	public void pruneAny() throws Exception {
		assertThat(new JacksonMultiAnyPathMaxStringLengthJsonFilter(-1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH);
	}	

	@Test
	public void maxStringLength() throws Exception {
		assertThat(new JacksonMultiAnyPathMaxStringLengthJsonFilter(DEFAULT_MAX_LENGTH, null, null)).hasMaxStringLength(DEFAULT_MAX_LENGTH);
	}
	
	@Test
	public void maxStringLengthAnonymizePrune() throws Exception {
		assertThat(new JacksonMultiAnyPathMaxStringLengthJsonFilter(DEFAULT_MAX_LENGTH, new String[]{"//key1"}, new String[]{"//key3"}))
			.hasMaxStringLength(DEFAULT_MAX_LENGTH)
			.hasPruned("//key3")
			.hasAnonymized("//key1");
	}
	
	@Test
	public void testConvenienceMethods() throws IOException {
		testConvenienceMethods(new JacksonMultiAnyPathMaxStringLengthJsonFilter(-1, null, null));
	}	
}
