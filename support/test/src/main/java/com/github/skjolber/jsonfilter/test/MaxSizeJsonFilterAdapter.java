package com.github.skjolber.jsonfilter.test;

import com.github.skjolber.jsonfilter.JsonFilter;

@FunctionalInterface
public interface MaxSizeJsonFilterAdapter {

	JsonFilter getMaxSize(int size);
}
