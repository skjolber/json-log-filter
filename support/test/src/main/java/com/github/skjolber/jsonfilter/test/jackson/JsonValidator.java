package com.github.skjolber.jsonfilter.test.jackson;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

public class JsonValidator {

	public static boolean isWellformed(String s) {
		return isWellformed(s, new JsonFactory());
	}

	public static boolean isWellformed(String s, JsonFactory jsonFactory) {
		try (JsonParser parser = jsonFactory.createParser(new StringReader(s))) {
			while(parser.nextToken() != null);
		} catch(Exception e) {
			return false;
		}
		return true;			
	}
	

	public static boolean isWellformed(byte[] s, JsonFactory jsonFactory) {
		try (JsonParser parser = jsonFactory.createParser(new ByteArrayInputStream(s))) {
			while(parser.nextToken() != null);
		} catch(Exception e) {
			return false;
		}
		return true;			
	}

}
