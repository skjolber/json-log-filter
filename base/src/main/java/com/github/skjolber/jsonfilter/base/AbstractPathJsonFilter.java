package com.github.skjolber.jsonfilter.base;

import java.nio.charset.StandardCharsets;

public abstract class AbstractPathJsonFilter extends AbstractJsonFilter {

	public enum FilterType {
		/** public for testing */
		ANON(AbstractRangesFilter.FILTER_ANON), PRUNE(AbstractRangesFilter.FILTER_PRUNE);
		
		private final int type;
		
		private FilterType(int type) {
			this.type = type;
		}
		
		public int getType() {
			return type;
		}
	}
	
	/** 
	 * group 1 starting with slash and containing no special chars except star (*). 
	 * optional group 2 starting with slash and containing no special chars except star (*) and at (@), must be last. 
	 */ 
	
	protected static final String SYNTAX_ABSOLUTE_PATH_SLASHES = "^\\/(.*)[^\\/]+$"; // slash + non-special chars '/' '*'
	protected static final String SYNTAX_ANY_PATH_SLASHES = "^(\\/\\/[^\\/|\\*]+)$"; // 2x slash + non-special chars '/' '*'

	protected static final String SYNTAX_ABSOLUTE_PATH_DOTS = "^\\.(.*)[^\\.]+$"; // slash + non-special chars '/' '*'
	protected static final String SYNTAX_ANY_PAT_DOTS = "^(\\.\\.[^\\.|\\*]+)$"; // 2x slash + non-special chars '/' '*'
	
	protected static final String[] EMPTY = new String[]{};
	protected static final String ANY_PREFIX_SLASHES = "//";
	protected static final String ANY_PREFIX_DOTS = "..";
	
	protected static final String STAR = "*";
	protected static final char[] STAR_CHARS = STAR.toCharArray();
	protected static final byte[] STAR_BYTES = new byte[] {'*'};
	
	public static boolean hasAnyPrefix(String[] filters) {
		if(filters != null) {
			for(String string : filters) {
				if(hasAnyPrefix(string)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean hasAnyPrefix(String string) {
		return string.startsWith(AbstractPathJsonFilter.ANY_PREFIX_SLASHES) || string.startsWith(AbstractPathJsonFilter.ANY_PREFIX_DOTS);
	}
	
	/** strictly not needed, but necessary for testing */
	protected final String[] anonymizes;
	protected final String[] prunes;
	
	protected final int maxPathMatches;
	
	public AbstractPathJsonFilter(int maxStringLength, int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, pruneMessage, anonymizeMessage, truncateMessage);
		
		if(maxPathMatches < -1) {
			throw new IllegalArgumentException();
		}
		
		this.maxPathMatches = maxPathMatches;
		
		if(anonymizes == null) {
			anonymizes = EMPTY;
		} else {
			validateAnonymizeExpressions(anonymizes);
		}
		if(prunes == null) {
			prunes = EMPTY;
		} else {
			validatePruneExpressions(prunes);
		}

		this.anonymizes = anonymizes;
		this.prunes = prunes;
	}

	public static void validateAnonymizeExpressions(String[] expressions) {
		for(String expression : expressions) {
			validateAnonymizeExpression(expression);
		}
	}

	public static void validateAnonymizeExpression(String expression) {
		if(!validateExpression(expression)) {
			throw new IllegalArgumentException("Illegal anonymize expression '" + expression + "'. Expected expression on the form /a/b/c or .a.b.c with wildcards or //a  or ..a without wildcards");
		}
	}

	private static boolean validateExpression(String expression) {
		if(!expression.contains("//") && !expression.contains("..")) {
			return expression.matches(SYNTAX_ABSOLUTE_PATH_DOTS) || expression.matches(SYNTAX_ABSOLUTE_PATH_SLASHES);
		}
		return expression.matches(SYNTAX_ANY_PAT_DOTS) || expression.matches(SYNTAX_ANY_PATH_SLASHES);
	}
	
	public static void validatePruneExpressions(String[] expressions) {
		for(String expression : expressions) {
			validatePruneExpression(expression);
		}
	}

	public static void validatePruneExpression(String expression) {
		if(!validateExpression(expression)) {
			throw new IllegalArgumentException("Illegal prune expression '" + expression + "'. Expected expression on the form /a/b/c or .a.b.c with wildcards or //a  or ..a without wildcards");
		}
	}
	
	protected static String[] parse(String expression) {
		String[] split = expression.split("/|\\.");
		String[] elementPath = new String[split.length - 1];
		for(int k = 0; k < elementPath.length; k++) {
			elementPath[k] = intern(split[k + 1]);
		}
		return elementPath;
	}
	
	public String[] getAnonymizeFilters() {
		return anonymizes;
	}

	public String[] getPruneFilters() {
		return prunes;
	}

	public static boolean matchPath(final char[] chars, int start, int end, final char[] attribute) {
		// check if wildcard
		if(attribute == STAR_CHARS) {
			return true;
		}
		int l = end - start;
		if(l < attribute.length) {
			return false;
		}
		if(attribute.length == l) {
			for(int i = 0; i < attribute.length; i++) {
				if(attribute[i] != chars[start + i]) {
					return false;
				}
			}
			return true;
		} 
		// check for escape
		// must be at least one escape within the attribute length
		int attributeOffset = 0;
		int sourceOffset = start;
		while(attributeOffset < attribute.length && sourceOffset < end) {
			if(chars[sourceOffset] == '\\') {
				sourceOffset++;
				if(chars[sourceOffset] == 'u') {
					// uXXXX
					sourceOffset++;
					
					if(HC[ (attribute[attributeOffset] >> 12) & 0xF] != chars[sourceOffset] ) {
						return false;
					}
					sourceOffset++;
					if(HC[ (attribute[attributeOffset] >> 8) & 0xF] != chars[sourceOffset] ) {
						return false;
					}
					sourceOffset++;
					if(HC[ (attribute[attributeOffset] >> 4) & 0xF] != chars[sourceOffset] ) {
						return false;
					}
					sourceOffset++;
					if(HC[ attribute[attributeOffset] & 0xF] != chars[sourceOffset] ) {
						return false;
					}
				} else {
					// n r t etc
					if(!isEscape(chars[sourceOffset], attribute[attributeOffset])) {
						return false;
					}
				}
			} else if(attribute[attributeOffset] != chars[sourceOffset]) {
				return false;
			}

			sourceOffset++;
			attributeOffset++;
		}
		return sourceOffset == end && attributeOffset == attribute.length;
	}
	
	public static boolean matchPath(final byte[] source, int start, int end, final byte[] attribute) {
		// check if wildcard
		if(attribute == STAR_BYTES) {
			return true;
		}
		int l = end - start;
		if(l < attribute.length) {
			return false;
		}
		if(attribute.length == l) {
			for(int i = 0; i < attribute.length; i++) {
				if(attribute[i] != source[start + i]) {
					return false;
				}
			}
			return true;
		}
		// check for escape
		// must be at least one escape within the attribute length
		int attributeOffset = 0;
		int sourceOffset = start;
		while(attributeOffset < attribute.length && sourceOffset < end) {
			if(source[sourceOffset] == '\\') {
				// this code will probably not run very often
				sourceOffset++;
				if(source[sourceOffset] == 'u') {
					// uXXXX
					sourceOffset++;

					// Direct comparison of UTF-8 and hexadecimal
					// 
					// https://tools.ietf.org/html/rfc8259:
					// Any character may be escaped.  If the character is in the Basic
					// Multilingual Plane (U+0000 through U+FFFF), then it may be
					// represented as a six-character sequence: a reverse solidus, followed
					// by the lowercase letter u, followed by four hexadecimal digits that
					// encode the character's code point.  The hexadecimal letters A though
					// F can be upper or lower case.  So, for example, a string containing
					// only a single reverse solidus character may be represented as
					// "\u005C".
					// 
					// https://no.wikipedia.org/wiki/UTF-8:
					// Three bytes are needed for characters in the rest of the Basic Multilingual Plane.
					//
					// 
					byte b = attribute[attributeOffset];
					if(b >= 0) { // single digit (ASCII)
						// 0xxxxxxx
						// 0xxx
						// 0   xxxx
						//
						// 007F
						if(source[sourceOffset++] != '0' || source[sourceOffset] != '0') {
							return false;
						}
						sourceOffset++;
						if(HC[(b >> 4) & 0xF] != source[sourceOffset] ) {
							return false;
						}
						sourceOffset++;
						if(HC[b & 0xF] != source[sourceOffset] ) {
							return false;
						}
					} else if ((b & 0xe0) == 0xc0) {
						// 110xxxxx 10xxxxxx
						//	xxx   10
						// 110   xx 10xx	
						// 110	  10  xxxx
						//
						// 07FF
						
						if(source[sourceOffset] != '0') {
							return false;
						}
						sourceOffset++;
						if(HC[ (b >> 2) & 0xF] != source[sourceOffset] ) {
							return false;
						}
						attributeOffset++;
						byte b2 = attribute[attributeOffset];
						sourceOffset++;
						if(HC[ ((b2 >> 4) & 0x3) | (b << 2) & 0xC] != source[sourceOffset] ) {
							return false;
						}
						sourceOffset++;
						if(HC[ b2 & 0xF] != source[sourceOffset] ) {
							return false;
						}
					} else if ((b & 0xf0) == 0xe0) {
						// 1110xxxx 10xxxxxx 10xxxxxx
						// 1110xxxx 10	   10
						// 1110	 10xxxx   10
						// 1110	 10	xx 10xx
						// 1110	 10	   10  xxxx
						//
						// FFFF
						if(HC[b & 0xF] != source[sourceOffset] ) {
							return false;
						}
						
						attributeOffset++;
						byte b2 = attribute[attributeOffset];
						
						sourceOffset++;
						if(HC[ (b2 >> 2) & 0xF] != source[sourceOffset] ) {
							return false;
						}

						attributeOffset++;
						byte b3 = attribute[attributeOffset];

						sourceOffset++;
						if(HC[ ((b3 >> 4) & 0x3) | (b2 << 2) & 0xC] != source[sourceOffset] ) {
							return false;
						}

						sourceOffset++;
						if(HC[ b3 & 0xF] != source[sourceOffset] ) {
							return false;
						}
					} else if ((b & 0xf8) == 0xf0) {
						// So outside the Basic Multilingual Plane
						// 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx

						// To escape an extended character that is not in the Basic Multilingual
						// Plane, the character is represented as a 12-character sequence,
						// encoding the UTF-16 surrogate pair.  So, for example, a string
						// containing only the G clef character (U+1D11E) may be represented as
						// "\uD834\uDD1E".
						int value = b & 0x07; // 3
						
						attributeOffset++;
						value = (value << 6) | (attribute[attributeOffset] & 0x3f); // 6
						attributeOffset++;
						value = (value << 6) | (attribute[attributeOffset] & 0x3f); // 6
						attributeOffset++;
						value = (value << 6) | (attribute[attributeOffset] & 0x3f); // 6
						
						 // 21 bits, 11 in the first, 10 in the second 
						 
						 /*
						cbuf[i++] = (char) (((code - 0x10000) >> 10) + 0xd800);
						cbuf[i++] = (char) (((code - 0x10000) & 0x3ff) + 0xdc00);						 
						 */
						 
						char a = (char) (((value - 0x10000) >> 10) + 0xd800);
						 
						if(HC[ (a >> 12) & 0xF] != source[sourceOffset] ) {
							return false;
						}
						sourceOffset++;
						if(HC[ (a >> 8) & 0xF] != source[sourceOffset] ) {
							return false;
						}
						sourceOffset++;
						if(HC[ (a >> 4) & 0xF] != source[sourceOffset] ) {
							return false;
						}
						sourceOffset++;
						if(HC[ a & 0xF] != source[sourceOffset] ) {
							return false;
						}
						
						sourceOffset++;
						
						if(sourceOffset >= end) {
							return false;
						}
						
						if(source[sourceOffset] != '\\') {
							return false;
						}
						
						sourceOffset++;
						if(source[sourceOffset] != 'u') {
							return false;
						}

						a = (char) (((value - 0x10000) & 0x3ff) + 0xdc00);			 

						sourceOffset++;
						if(HC[ (a >> 12) & 0xF] != source[sourceOffset] ) {
							return false;
						}
						sourceOffset++;
						if(HC[ (a >> 8) & 0xF] != source[sourceOffset] ) {
							return false;
						}
						sourceOffset++;
						if(HC[ (a >> 4) & 0xF] != source[sourceOffset] ) {
							return false;
						}
						sourceOffset++;
						if(HC[ a & 0xF] != source[sourceOffset] ) {
							return false;
						}						 
					} else {
						return false;
					}
				} else {
					// n r t etc
					if(!isEscape(source[sourceOffset], attribute[attributeOffset])) {
						return false;
					}
				}
			} else if(attribute[attributeOffset] != source[sourceOffset]) {
				return false;
			}

			sourceOffset++;
			attributeOffset++;
		}
		return sourceOffset == end && attributeOffset == attribute.length;
	}	
	
	protected static boolean isEscape(int c, int expected) {
		switch(c) {
			case '"' : return '"' == expected;
			case '\\' : return '\\' == expected;
			case '/' : return '/' == expected;
			case 'b' : return 0x08 == expected;
			case 't' : return 0x09 == expected;
			case 'f' : return 0x0C == expected;
			case 'n' : return 0x0A == expected;
			case 'r' : return 0x0D == expected;
			default : return false;
		}
	}
	
	/**
	 * 
	 * Note: Expects input to be unencoded
	 * 
	 * @param chars text to test
	 * @param attribute text to match
	 * @return true if matches
	 */
	
	public static boolean matchPath(final String chars, final String attribute) {
		// check if wildcard, assume interned locally
		if(attribute == STAR) {
			return true;
		}
		return chars.equals(attribute);
	}
	
	protected static char[][] toCharArray(String[] pathStrings) {
		char[][] paths = new char[pathStrings.length][];
		for(int i = 0; i < pathStrings.length; i++) {
			paths[i] = intern(pathStrings[i].toCharArray());
		}
		return paths;
	}
	
	protected static byte[][] toByteArray(String[] pathStrings) {
		byte[][] paths = new byte[pathStrings.length][];
		for(int i = 0; i < pathStrings.length; i++) {
			paths[i] = intern(pathStrings[i].getBytes(StandardCharsets.UTF_8));
		}
		return paths;
	}	
	
	public static String intern(String string) {
		if(string.equals(STAR)) {
			return STAR;
		} else {
			return string;
		}
	}
	
	public static char[] intern(char[] chars) {
		if(chars.length == 1 && chars[0] == '*') {
			return STAR_CHARS;
		} else {
			return chars;
		}
	}	

	public static byte[] intern(byte[] bytes) {
		if(bytes.length == 1 && bytes[0] == '*') {
			return STAR_BYTES;
		} else {
			return bytes;
		}
	}	

	public int getMaxPathMatches() {
		return maxPathMatches;
	}
	
	protected String[] getAnonymizes() {
		return anonymizes;
	}
	
	protected String[] getPrunes() {
		return prunes;
	}
}
