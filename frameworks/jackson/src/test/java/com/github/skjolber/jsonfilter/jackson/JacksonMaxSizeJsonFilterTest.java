package com.github.skjolber.jsonfilter.jackson;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

public class JacksonMaxSizeJsonFilterTest extends AbstractJacksonJsonFilterTest {

	public JacksonMaxSizeJsonFilterTest() throws Exception {
		super();
	}

	public static void main(String[] args) throws IOException {
		
		File file = new File("../../support/test/src/main/resources/json/object/objectBefore.json");
		String string = IOUtils.toString(file.toURI(), StandardCharsets.UTF_8);
		
		System.out.println(string);

		for(int i = 2; i < string.length(); i++) {
		
			JacksonMaxSizeJsonFilter filter = new JacksonMaxSizeJsonFilter(i);
		
			String process = filter.process(string);
			
			System.out.println(i + " " + process);
		}
	}
	
}
