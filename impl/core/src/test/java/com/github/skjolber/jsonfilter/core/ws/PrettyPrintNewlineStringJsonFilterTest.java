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
import com.github.skjolber.jsonfilter.core.pp.Indent;
import com.github.skjolber.jsonfilter.core.pp.PrettyPrintingJsonFilter;
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
		FileInputStream fileInputStream = new FileInputStream(new File("/home/skjolber/git/json-log-filter-github/impl/core/src/test/resources/json/wiki/person.json"));
		String string = IOUtils.toString(fileInputStream, StandardCharsets.UTF_8);
		
		Indent indent = Indent.newBuilder().build();
		PrettyPrintingJsonFilter pp = new PrettyPrintingJsonFilter(indent);
		
		String whitespaceString = pp.process(string);
		System.out.println("**************************************");
		System.out.println(whitespaceString);
		System.out.println("**************************************");
		
		JsonFilter filter = new MaxStringLengthPrettyPrintJsonFilter(DEFAULT_MAX_STRING_LENGTH);
		
		System.out.println(filter.getClass().getSimpleName() + " " + filter.process(whitespaceString));
	}

}
