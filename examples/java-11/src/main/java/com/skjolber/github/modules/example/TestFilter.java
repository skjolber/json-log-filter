package com.skjolber.github.modules.example;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;
import com.github.skjolber.jsonfilter.core.DefaultJsonLogFilterBuilder;

public class TestFilter {

	public static final void main(String[] args) {
		
		JsonFilter filter = DefaultJsonLogFilterBuilder.createInstance()
                .withMaxStringLength(127)
                .withAnonymize("/customer/email") // inserts ***** for values
                .withPrune("/customer/account") // removes whole subtree
                .withMaxPathMatches(10)
                .build();
		
		System.out.println("Got filter " + filter.getClass().getName());
	}
}
