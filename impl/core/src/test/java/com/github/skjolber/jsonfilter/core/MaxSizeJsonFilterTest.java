package com.github.skjolber.jsonfilter.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class MaxSizeJsonFilterTest extends DefaultJsonFilterTest {

	public MaxSizeJsonFilterTest() throws Exception {
		super();
	}
		
	public static void main(String[] args) throws IOException {
		
		File file = new File("../../support/test/src/main/resources/json/object/objectBefore.json");
		String string = IOUtils.toString(file.toURI(), StandardCharsets.UTF_8);
		
		System.out.println(string);

		
		for(int i = 2; i < string.length(); i++) {
		
			MaxSizeJsonFilter filter = new MaxSizeJsonFilter(i);
		
			String process = filter.process(string);
			
			System.out.println(i + " " + process);
		}
	}
}
