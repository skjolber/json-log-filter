package com.github.skjolber.jsonfilter.core.pp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

public class PrettyPrintingJsonFilterTest {

	@Test
	public void testDeepStructure5() throws IOException {
		FileInputStream fileInputStream = new FileInputStream(new File("/home/skjolber/git/json-log-filter-github/impl/core/src/test/resources/json/wiki/person.json"));
		String string = IOUtils.toString(fileInputStream, StandardCharsets.UTF_8);
		
		PrettyPrintingJsonFilter pp = new PrettyPrintingJsonFilter(Indent.newBuilder().build());
		
		String whitespaceString = pp.process(string);
		System.out.println("**************************************");
		System.out.println(whitespaceString);
		System.out.println("**************************************");
	}

}
