package com.github.skjolber.jsonfilter.core.ws;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.ws.PrettyPrintJsonFilter;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class PrettyPrintNewlineStringJsonFilterTest extends DefaultJsonFilterTest {

	public PrettyPrintNewlineStringJsonFilterTest() throws Exception {
		super();
	}
	
	@Test
	public void testDeepStructure4() throws IOException {
		String string = readResource("/json/wiki/person.prettyprinted.json");
		
		JsonFilter filter = new PrettyPrintJsonFilter();

		System.out.println(filter.getClass().getSimpleName() + " " + filter.process(string));
		
		JsonFactory factory = new JsonFactory();

		JsonParser parser = factory.createJsonParser(new StringReader(string));
		
		JsonToken token = null;
		while ((token=parser.nextToken()) != null);
	}

	@Test
	public void testDeepStructure5() throws IOException {
		FileInputStream fileInputStream = new FileInputStream(new File("./../../support/test/src/main/resources/json/array/1d/anyArray.json"));
		String string = IOUtils.toString(fileInputStream, StandardCharsets.UTF_8);
		
		JsonFilter filter = new MaxStringLengthPrettyPrintJsonFilter(5);

		System.out.println(filter.getClass().getSimpleName() + " " + filter.process(string));
	}

}
