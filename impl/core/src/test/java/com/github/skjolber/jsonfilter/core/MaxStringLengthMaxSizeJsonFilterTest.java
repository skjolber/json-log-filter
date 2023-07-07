package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.MaxSizeJsonFilterFunction;

public class MaxStringLengthMaxSizeJsonFilterTest extends DefaultJsonFilterTest {

	public MaxStringLengthMaxSizeJsonFilterTest() throws Exception {
		super();
	}
	
	@Test
	@ResourceLock(value = "jackson")
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MaxStringLengthMaxSizeJsonFilter(128, size));
	}

	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new MaxStringLengthMaxSizeJsonFilter(128, size));
	}

	@Test
	public void passthrough_success() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MaxStringLengthMaxSizeJsonFilter(-1, size);

		assertThatMaxSize(maxSize, new MaxStringLengthJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void maxStringLength() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MaxStringLengthMaxSizeJsonFilter(DEFAULT_MAX_STRING_LENGTH, size);
		
		assertThatMaxSize(maxSize, new MaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH).hasMaxStringLengthMetrics();
	}
	
	@Test
	public void exception_returns_false() throws Exception {
		JsonFilter filter = new MaxStringLengthMaxSizeJsonFilter(-1, -1);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertNull(filter.process(new byte[] {}, 1, 1));
	}	

}
