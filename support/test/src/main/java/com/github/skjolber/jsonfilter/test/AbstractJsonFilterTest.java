package com.github.skjolber.jsonfilter.test;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.github.skjolber.jsonfilter.JsonFilter;

/**
 * 
 * Abstract test class.
 *
 */

public abstract class AbstractJsonFilterTest {
	
	protected String DEEP_PATH = "/f0/f1/f2/f3/f4/f5/f6/f7/f8/f9/f10/f11/f12/f13/f14/f15/f16/f17/f18/f19/f20/f21/f22/f23/f24/f25/f26/f27/f28/f29/f30/f31/f32/f33";

	protected JsonFactory factory = new JsonFactory();

	protected Map<String, byte[]> maps = new HashMap<>();
	
	protected JsonFilterRunner runner;
	
	public AbstractJsonFilterTest(JsonFilterRunner runner) {
		this.runner = runner;
	}
	protected JsonFilterResultSubject assertThat(JsonFilter filter) throws Exception {
		return assertThat(filter, (s) -> true);
	}
	
	protected JsonFilterResultSubject assertThat(JsonFilter filter, Predicate<String> predicate) throws Exception {
		
		JsonFilterResult process = runner.process(filter, predicate);
			
		return JsonFilterResultSubject.assertThat(process);
	}

	protected JsonFilterResultSubject assertThatMaxSize(Function<Integer, JsonFilter> maxSize, JsonFilter infiniteSize) throws Exception {
		return assertThatMaxSize(maxSize, infiniteSize, (p) -> true);
	}

	protected JsonFilterResultSubject assertThatMaxSize(Function<Integer, JsonFilter> maxSize, JsonFilter infiniteSize, Predicate<String> filter) throws Exception {
		JsonFilterResult process = runner.process(maxSize, infiniteSize, filter);
			
		return JsonFilterResultSubject.assertThat(process);
	}
	
	public void validate(String resource, Function<Integer, JsonFilter> filter) throws IOException {
		
		byte[] bs = maps.get(resource);
		if(bs == null) {
			bs = IOUtils.toByteArray(getClass().getResourceAsStream(resource));
			maps.put(resource,  bs);
		}
		
		int factor = 3;
		
		for(int i = 2; i < bs.length / factor; i++) {
			JsonFilter apply = filter.apply(i);

			byte[] byteArray = apply.process(bs, 0, bs.length);
			
			try {
				validate(byteArray);
			} catch(JsonParseException e) {
				System.out.println(new String(byteArray));
				fail(byteArray.length + " vs " + i);
			}
			if(byteArray.length >= i + 16) {
				System.out.println(new String(byteArray));
				fail(byteArray.length + " vs " + i);
			}
			
			if(i % 10000 == 0) {
				System.out.println("Bytes " + i);
			}

		}
		
		char[] charArray = new String(bs).toCharArray();

		StringBuilder bout = new StringBuilder(charArray.length);

		for(int i = 2; i < bs.length / factor; i++) {
			JsonFilter apply = filter.apply(i);

			apply.process(charArray, 0, charArray.length, bout);
			
			String result = bout.toString();
			bout.setLength(0);
			try {
				validate(result);
			} catch(JsonParseException e) {
				System.out.println(result);
				fail(bout.length() + " vs " + i );
			}
			if(result.length() >= i + 16) {
				System.out.println(result);
				fail(result.length() + " vs " + i);
			}
			
			if(i % 10000 == 0) {
				System.out.println("Chars " + i);
			}

		}
		
	}
	
	public void validateDeepStructure(Function<Integer, JsonFilter> filter) throws IOException {
		int levels = 100;
		byte[] generateDeepStructure = Generator.generateDeepObjectStructure(levels);
		validate(filter, levels, generateDeepStructure);
		
		generateDeepStructure = Generator.generateDeepArrayStructure(levels);
		validate(filter, levels, generateDeepStructure);

		generateDeepStructure = Generator.generateDeepMixedStructure(levels);
		validate(filter, levels, generateDeepStructure);

	}

	private void validate(Function<Integer, JsonFilter> filter, int levels, byte[] generateDeepStructure)
			throws IOException, JsonParseException {
		for(int i = 2; i < generateDeepStructure.length; i++) {		
			JsonFilter apply = filter.apply(i);
		
			byte[] process = apply.process(generateDeepStructure);
	
			assertTrue(process.length <= i + levels);
			
			validate(process);
		}

		String string = new String(generateDeepStructure, StandardCharsets.UTF_8);
		for(int i = 2; i < string.length(); i++) {		
			JsonFilter apply = filter.apply(i);
		
			String process = apply.process(string);
	
			assertTrue(process.length() <= i + levels);

			validate(process);
		}
	}

	protected void validate(byte[] process) throws IOException, JsonParseException {
		try (JsonParser parse = factory.createParser(process)) {
			while(parse.nextToken() != null);
		}
	}

	protected void validate(String process) throws IOException, JsonParseException {
		try (JsonParser parse = factory.createParser(process)) {
			while(parse.nextToken() != null);
		}
	}
	
	public String readResource(String path) throws IOException {
		return IOUtils.toString(getClass().getResourceAsStream(path), StandardCharsets.UTF_8);
	}
	

}
