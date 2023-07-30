package com.github.skjolber.jsonfilter.test.jackson;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

/**
 * 
 * Escape newlines so to make JSON comparisons easier to read
 * 
 */

public class JsonCompactor {

	public static byte[] compact(byte[] json) {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		for (byte b : json) {
			if(b == '\n') {
				bout.write('\\');
				bout.write('n');
			} else {
				bout.write(b);
			}
		}
		
		return bout.toByteArray();
	}
	
	public static String compact(CharSequence json) {
		StringWriter writer = new StringWriter();

		for (int i = 0; i < json.length(); i++) {
			char b = json.charAt(i);
			if(b == '\n') {
				writer.append('\\');
				writer.append('n');
			} else {
				writer.append(b);
			}
		}
		
		return writer.toString();
	}

}
