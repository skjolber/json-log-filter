package com.github.skjolber.jsonfilter.test;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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

	protected JsonFactory factory = new JsonFactory();

	protected Map<String, byte[]> maps = new HashMap<>();
	
	protected JsonFilterRunner runner;
	
	public AbstractJsonFilterTest(JsonFilterRunner runner) {
		this.runner = runner;
	}
	
	protected JsonFilterResultSubject assertThat(JsonFilter filter) throws Exception {
		JsonFilterResult process = runner.process(filter);
			
		return JsonFilterResultSubject.assertThat(process);
	}
	
	protected JsonFilterResultSubject assertThatMaxSize(Function<Integer, JsonFilter> maxSize, JsonFilter infiniteSize) throws Exception {
		JsonFilterResult process = runner.process(maxSize, infiniteSize);
			
		return JsonFilterResultSubject.assertThat(process);
	}
	
	public void validate(String resource, Function<Integer, JsonFilter> filter) throws IOException {
		
		byte[] bs = maps.get(resource);
		if(bs == null) {
			bs = IOUtils.toByteArray(getClass().getResourceAsStream(resource));
			maps.put(resource,  bs);
		}
		
		for(int i = 2; i < bs.length; i++) {
			JsonFilter apply = filter.apply(i);

			ByteArrayOutputStream bout = new ByteArrayOutputStream(i);
			apply.process(bs, 0, bs.length, bout);
			
			byte[] byteArray = bout.toByteArray();
			
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
		}
		
		char[] charArray = new String(bs).toCharArray();		
		for(int i = 2; i < charArray.length; i++) {
			JsonFilter apply = filter.apply(i);

			StringBuilder bout = new StringBuilder(i);
			apply.process(charArray, 0, charArray.length, bout);
			
			String result = bout.toString();
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
	

}
