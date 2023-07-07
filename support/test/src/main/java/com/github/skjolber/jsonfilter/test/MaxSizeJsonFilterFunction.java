package com.github.skjolber.jsonfilter.test;

import com.github.skjolber.jsonfilter.JsonFilter;

@FunctionalInterface
public interface MaxSizeJsonFilterFunction {

	JsonFilter getMaxSize(int size);
}
