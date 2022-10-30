package com.github.skjolber.jsonfilter.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;

public class SkipSubtreeJsonFilter extends AbstractJsonFilter {

	public SkipSubtreeJsonFilter(int maxStringLength, String pruneJson, String anonymizeJson,
			String truncateJsonString) {
		super(maxStringLength, -1, pruneJson, anonymizeJson, truncateJsonString);
	}

	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		output.append(chars, offset, length);
		return skipSubtree(chars, offset) == offset + length;
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
		output.write(chars, offset, length);
		return skipSubtree(chars, offset) == offset + length;
	}
	

	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output,
			JsonFilterMetrics filterMetrics) {
		return process(chars, offset, length, output);
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output,
			JsonFilterMetrics filterMetrics) {
		return process(chars, offset, length, output);
	}


	public static int skipSubtree(char[] chars, int offset) {
		int level = 0;

		while(true) {
			switch(chars[offset]) {
				case '[' : 
				case '{' : {
					level++;
					break;
				}
	
				case ']' : 
				case '}' : {
					level--;
					
					if(level == 0) {
						return offset + 1;
					} else if(level < 0) { // was scalar value
						return offset;
					}
					break;
				}
				case ',' : {
					if(level == 0) { // was scalar value
						return offset;
					}
					break;
				}
				case '"' : {
					do {
						offset++;
					} while(chars[offset] != '"' || chars[offset - 1] == '\\');
					
					if(level == 0) { 
						return offset + 1;
					}
					break;
				}
				default :
			}
			offset++;
		}
	}


	public static int skipSubtree(byte[] chars, int offset) {
		int level = 0;

		while(true) {
			switch(chars[offset]) {
				case '[' : 
				case '{' : {
					level++;
					break;
				}
	
				case ']' : 
				case '}' : {
					level--;
					
					if(level == 0) {
						return offset + 1;
					} else if(level < 0) { // was scalar value
						return offset;
					}
					break;
				}
				case ',' : {
					if(level == 0) { // was scalar value
						return offset;
					}
					break;
				}
				case '"' : {
					do {
						offset++;
					} while(chars[offset] != '"' || chars[offset - 1] == '\\');
					
					if(level == 0) {
						return offset + 1;
					}
					break;
				}
				default :
			}
			offset++;
		}
	}
	

}
