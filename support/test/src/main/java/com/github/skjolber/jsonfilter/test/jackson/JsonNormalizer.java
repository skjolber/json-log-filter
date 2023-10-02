package com.github.skjolber.jsonfilter.test.jackson;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.io.CharTypes;

/**
 * 
 * High surrogate characters might be escaped or encoded as the appropriate UTF-8 sequence. 
 * Jackson filters escapes these characters, while the core filters keep them as is in the input.
 * As a consequence, the max string length calculations might differ somewhat in the actual
 * characters removed (plus the count in the resulting truncate message).
 */

public class JsonNormalizer {

    private final static char BYTE_u = 'u';
    private final static char BYTE_0 = '0';
    private final static char BYTE_BACKSLASH = '\\';

    private final static char[] HEX_CHARS = CharTypes.copyHexChars();

    public static String normalize(byte[] value) {
    	return normalize(new String(value, StandardCharsets.UTF_8));
    }

    public static String normalize(String value) {
		if(isHighSurrogate(value) || isEscape(value)) {
			String filtered = filterMaxStringLength(value);

			StringBuilder sb = new StringBuilder(filtered.length());
			
			for(int i = 0; i < filtered.length(); i++) {
				if(Character.isHighSurrogate(filtered.charAt(i))) {
					writeGenericEscape(filtered.charAt(i), sb);
					writeGenericEscape(filtered.charAt(i + 1), sb);
					i++;
				} else {
					sb.append(filtered.charAt(i));
				}
			}
			
			return sb.toString();
		} else {
			return filterMaxStringLength(value);
		}
	}

	public static String filterMaxStringLength(String result) {
		// ...TRUNCATED BY 

		Pattern patt = Pattern.compile("(TRUNCATED BY )([0-9]+)");
		Matcher m = patt.matcher(result);
		StringBuffer sb = new StringBuffer(result.length());
		while (m.find()) {
			String text = m.group(1);
			m.appendReplacement(sb, text + "XX");
		}
		m.appendTail(sb);
		return sb.toString();
	}

	public static boolean isHighSurrogate(String from) {
		for(int i = 0; i < from.length(); i++) {
			if(Character.isHighSurrogate(from.charAt(i))) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isEscape(String from) {
		loop:
		for(int i = 0; i < from.length() - 6; i++) {
			if(from.charAt(i) == '\\') {
				if(from.charAt(i + 1) == 'u') {
				
					for(int k = 0; k < 4; k++) {
						if(!Character.isDigit(from.charAt(i + 2 + k))) {
							continue loop;
						}
					}
					
					return true;
				} else {
					switch(from.charAt(i + 1)) {
					case '"':
					case '\\':
					case '/':
					case 'b':
					case 'f':
					case 'n':
					case 'r':
					case 't':
						return true;
					default : 
						
					}
				}
			}
		}
		return false;
	}

    /**
     * Method called to write a generic Unicode escape for given character.
     * 
     * @param charToEscape Character to escape using escape sequence (\\uXXXX)
     * @param builder escape target
     */
    
    public static void writeGenericEscape(int charToEscape, StringBuilder builder) {
    	builder.append(BYTE_BACKSLASH);
    	builder.append(BYTE_u);
    	
        if (charToEscape > 0xFF) {
            int hi = (charToEscape >> 8) & 0xFF;
            builder.append(HEX_CHARS[hi >> 4]);
            builder.append(HEX_CHARS[hi & 0xF]);
            charToEscape &= 0xFF;
        } else {
        	builder.append(BYTE_0);
        	builder.append(BYTE_0);
        }
        // We know it's a control char, so only the last 2 chars are non-0
        builder.append(HEX_CHARS[charToEscape >> 4]);
        builder.append(HEX_CHARS[charToEscape & 0xF]);
    }

}
