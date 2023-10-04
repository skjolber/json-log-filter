package com.github.skjolber.jsonfilter.base;

import java.nio.charset.StandardCharsets;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

public abstract class AbstractJsonFilter implements JsonFilter {

	public static final String FILTER_PRUNE_MESSAGE = "SUBTREE REMOVED";
	public static final String FILTER_ANONYMIZE = "*****";
	public static final String FILTER_TRUNCATE_MESSAGE = "...TRUNCATED BY ";

	public static final String FILTER_PRUNE_MESSAGE_JSON = '"' + FILTER_PRUNE_MESSAGE + '"';
	public static final String FILTER_ANONYMIZE_JSON = '"' + FILTER_ANONYMIZE + '"';

	protected static final int MAX_STRING_LENGTH = Integer.MAX_VALUE - 2;

	protected final int maxStringLength; // not always in use, if so set to max int
	protected final int maxSize; // not always in use, if so set to max int
	
	protected final char[] pruneJsonValue;
	protected final char[] anonymizeJsonValue;
	protected final char[] truncateStringValue;
	
	protected final byte[] pruneJsonValueAsBytes;
	protected final byte[] anonymizeJsonValueAsBytes;
	protected final byte[] truncateStringValueAsBytes;
	
	public AbstractJsonFilter(int maxStringLength, int maxSize, String pruneJson, String anonymizeJson, String truncateJsonString) {
		if(maxStringLength < -1 || maxStringLength > MAX_STRING_LENGTH) {
			throw new IllegalArgumentException("Expected -1 or positive integer lower than Integer.MAX_VALUE - 1");
		}
		
		if(maxStringLength == -1) {
			this.maxStringLength = MAX_STRING_LENGTH; // make room for quotes, without overflow
		} else {
			this.maxStringLength = maxStringLength;
		}
		if(maxSize == -1) {
			this.maxSize = Integer.MAX_VALUE;
		} else {
			this.maxSize = maxSize;
		}
		
		this.pruneJsonValue = pruneJson.toCharArray();
		this.anonymizeJsonValue = anonymizeJson.toCharArray();
		this.truncateStringValue = truncateJsonString.toCharArray();
		
		this.pruneJsonValueAsBytes = pruneJson.getBytes(StandardCharsets.UTF_8);
		this.anonymizeJsonValueAsBytes = anonymizeJson.getBytes(StandardCharsets.UTF_8);
		this.truncateStringValueAsBytes = truncateJsonString.getBytes(StandardCharsets.UTF_8);
	}

	protected boolean mustConstrainMaxSize(int length) {
		return length > maxSize;
	}
	
	public boolean process(String jsonString, StringBuilder output) {
		char[] chars = jsonString.toCharArray();
		
		return process(chars, 0, chars.length, output);
	}
	
	public String process(char[] chars) {
		StringBuilder output = new StringBuilder(chars.length);
		
		if(process(chars, 0, chars.length, output)) {
			return output.toString();
		}
		return null;
	}

	@Override
	public byte[] process(byte[] chars) {
		return process(chars, 0, chars.length);
	}
	
	@Override
	public byte[] process(byte[] chars, int offset, int length) {
		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(chars.length);
		
		if(process(chars, offset, length, output)) {
			return output.toByteArray();
		}
		return null;
	}
	
	public String process(String jsonString) {
		return process(jsonString.toCharArray());
	}
	
	public boolean process(String jsonString, StringBuilder output, JsonFilterMetrics metrics) {
		char[] chars = jsonString.toCharArray();
		
		return process(chars, 0, chars.length, output, metrics);
	}
	
	public String process(char[] chars, JsonFilterMetrics metrics) {
		StringBuilder output = new StringBuilder(chars.length);
		
		if(process(chars, 0, chars.length, output, metrics)) {
			return output.toString();
		}
		return null;
	}

	@Override
	public byte[] process(byte[] chars, JsonFilterMetrics metrics) {
		return process(chars, 0, chars.length, metrics);
	}
	
	@Override
	public byte[] process(byte[] chars, int offset, int length, JsonFilterMetrics metrics) {
		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(chars.length);
		
		if(process(chars, offset, length, output, metrics)) {
			return output.toByteArray();
		}
		return null;
	}
	
	public String process(String jsonString, JsonFilterMetrics metrics) {
		return process(jsonString.toCharArray(), metrics);
	}
	
	/**
	 * Method that will quote text contents using JSON standard quoting,
	 * and append results to a supplied {@link StringBuilder}.
	 * Use this variant if you have e.g. a {@link StringBuilder} and want to avoid superfluous copying of it.
	 * @param input source
	 * @param output destination
	 * From the Jackson library.
	 */
	public static void quoteAsString(CharSequence input, StringBuilder output) {
		final int[] escCodes = OUTPUT_ESCAPES_128;
		int inPtr = 0;
		final int inputLen = input.length();

		while (inPtr < inputLen) {
			while (true) {
				char c = input.charAt(inPtr);
				if (c < OUTPUT_ESCAPE_128_LENGTH && escCodes[c] != 0) {
					break;
				}
				output.append(c);
				if (++inPtr >= inputLen) {
					return;
				}
			}
			// something to escape; 2 or 6-char variant?
			char d = input.charAt(inPtr++);
			int escCode = escCodes[d];
			if(escCode < 0) {
				// \\u00XX
				output.append(NUMERIC_PREFIX); // 0-3:
				output.append(HC[d >> 4]); // 4
				output.append(HC[d & 0xF]); // 5
			} else {
				output.append('\\'); // 0
				output.append((char) escCode); // 1:
			}
		}
	}

	public static void quoteAsString(char[] input, int inPtr, int inputLen, StringBuilder output) {
		final int[] escCodes = OUTPUT_ESCAPES_128;

		while (inPtr < inputLen) {
			while (true) {
				char c = input[inPtr];
				if (c < OUTPUT_ESCAPE_128_LENGTH && escCodes[c] != 0) {
					break;
				}
				output.append(c);
				if (++inPtr >= inputLen) {
					return;
				}
			}
			// something to escape; 2 or 6-char variant?
			char d = input[inPtr++];
			int escCode = escCodes[d];
			if(escCode < 0) {
				// \\u00XX
				output.append(NUMERIC_PREFIX); // 0-3:
				output.append(HC[d >> 4]); // 4
				output.append(HC[d & 0xF]); // 5
			} else {
				output.append('\\'); // 0
				output.append((char) escCode); // 1:
			}
		}
	}
	
	
	/**
	 * Lookup table used for determining which output characters in
	 * 7-bit ASCII range need to be quoted.
	 */
	protected static final int[] OUTPUT_ESCAPES_128;
	protected static final int OUTPUT_ESCAPE_128_LENGTH = 93; // slash + 1
	private static final char[] NUMERIC_PREFIX = new char[]{'\\', 'u', '0', '0'};
	static {
		int[] table = new int[OUTPUT_ESCAPE_128_LENGTH];
		// Control chars need generic escape sequence
		for (int i = 0; i < 32; ++i) {
			// 04-Mar-2011, tatu: Used to use "-(i + 1)", replaced with constant
			table[i] = -1;
		}
		// Others (and some within that range too) have explicit shorter sequences
		table['"'] = '"';
		table['\\'] = '\\';
		// Escaping of slash is optional, so let's not add it
		table[0x08] = 'b';
		table[0x09] = 't';
		table[0x0C] = 'f';
		table[0x0A] = 'n';
		table[0x0D] = 'r';
		OUTPUT_ESCAPES_128 = table;
	}
	
	protected final static char[] HC = "0123456789ABCDEF".toCharArray();
	protected final static byte[] HB;
	static {
		int len = HC.length;
		HB = new byte[len];
		for (int i = 0; i < len; ++i) {
			HB[i] = (byte) HC[i];
		}
	}   

	protected char[] getPruneJsonValue() {
		return pruneJsonValue;
	}
	
	protected char[] getAnonymizeJsonValue() {
		return anonymizeJsonValue;
	}
	
	protected char[] getTruncateStringValue() {
		return truncateStringValue;
	}

	public int getMaxStringLength() {
		return maxStringLength;
	}

	public int getMaxSize() {
		return maxSize;
	}
	

	public static int lengthToDigits(int number) {
		if (number < 100000) {
		    if (number < 100) {
		        if (number < 10) {
		            return 1;
		        } else {
		            return 2;
		        }
		    } else {
		        if (number < 1000) {
		            return 3;
		        } else {
		            if (number < 10000) {
		                return 4;
		            } else {
		                return 5;
		            }
		        }
		    }
		} else {
		    if (number < 10000000) {
		        if (number < 1000000) {
		            return 6;
		        } else {
		            return 7;
		        }
		    } else {
		        if (number < 100000000) {
		            return 8;
		        } else {
		            if (number < 1000000000) {
		                return 9;
		            } else {
		                return 10;
		            }
		        }
		    }
		}
	}
	
}
