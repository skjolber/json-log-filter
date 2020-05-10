package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.JsonFilter;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class JsonLogFilterBuilderTest {

	@Test
	public void testBuilder() {
		JsonFilter filter = JsonLogFilterBuilder.createInstance()
                .withMaxStringLength(127)
                .withAnonymize("/customer/email") // inserts ***** for values
                .withPrune("/customer/account") // removes whole subtree
                .build();
		assertNotNull(filter);
	}
}
