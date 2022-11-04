package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.core.pp.Indent;
import com.github.skjolber.jsonfilter.core.pp.PrettyPrintingJsonFilter;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class MaxStringLengthRemoveWhitespaceJsonFilterTest  extends DefaultJsonFilterTest {

	private final static PrettyPrintingJsonFilter pp = new PrettyPrintingJsonFilter(Indent.newBuilder().build());

	public MaxStringLengthRemoveWhitespaceJsonFilterTest() throws Exception {
		super(false, true);
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new MaxStringLengthRemoveWhitespaceJsonFilter(-1), pp).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new MaxStringLengthRemoveWhitespaceJsonFilter(-1).process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(new MaxStringLengthRemoveWhitespaceJsonFilter(-1).process(new byte[] {}, 1, 1, new ByteArrayOutputStream()));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		MaxStringLengthRemoveWhitespaceJsonFilter maxStringLengthJsonFilter = new MaxStringLengthRemoveWhitespaceJsonFilter(-1);
		assertNull(maxStringLengthJsonFilter.process(TRUNCATED));
		assertNull(maxStringLengthJsonFilter.process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
		
		assertNull(new MaxStringLengthRemoveWhitespaceJsonFilter(-1).process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}
	
	@Test
	public void maxStringLength() throws Exception {
		assertThat(new MaxStringLengthRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH), pp).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH);
	}
	
}
