package com.github.skjolber.jsonfilter.base;

public abstract class AbstractPathJsonFilter extends AbstractRangesJsonFilter {

	public enum FilterType {
		/** public for testing */
		ANON(CharArrayFilter.FILTER_ANON), PRUNE(CharArrayFilter.FILTER_PRUNE);
		
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
	protected static final String pruneSyntaxAbsolutePath = "^(\\/([^\\/]+)|\\*)+$"; // slash + non-special chars '/' '*'
	protected static final String syntaxAnyPath = "^(\\/\\/[^\\/|\\*]+)$"; // 2x slash + non-special chars '/' '*'

	protected static final String[] EMPTY = new String[]{};
	public static final String ANY_PREFIX = "//";
	
	public static final String STAR = "*";
	public static final char[] STAR_CHARS = STAR.toCharArray();
	
	/** strictly not needed, but necessary for testing */
	protected final String[] anonymizes;
	protected final String[] prunes;
	protected final int maxPathMatches;
	
	public AbstractPathJsonFilter(int maxStringLength, int maxPathMatches, String[] anonymizes, String[] prunes) {
		super(maxStringLength);
		
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

	public AbstractPathJsonFilter(String[] anonymizes, String[] prunes) {
		this(-1, -1, anonymizes, prunes);
	}

	public static void validateAnonymizeExpressions(String[] expressions) {
		for(String expression : expressions) {
			validateAnonymizeExpression(expression);
		}
	}

	public static void validateAnonymizeExpression(String expression) {
		if(!expression.matches(pruneSyntaxAbsolutePath) && !expression.matches(syntaxAnyPath)) {
			throw new IllegalArgumentException("Illegal expression '" + expression + "'. Expected expression on the form /a/b/c with wildcards or //a without wildcards");
		}
	}
	
	public static void validatePruneExpressions(String[] expressions) {
		for(String expression : expressions) {
			validatePruneExpression(expression);
		}
	}

	public static void validatePruneExpression(String expression) {
		if(!expression.matches(pruneSyntaxAbsolutePath) && !expression.matches(syntaxAnyPath) ) {
			throw new IllegalArgumentException("Illegal expression '" + expression + "'. Expected expression on the form /a/b/c with wildcards or //a without wildcards");
		}
	}
	
	protected static String[] parse(String expression) {
		String[] split = expression.split("/");
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
		if(attribute.length == 1 && attribute[0] == '*') {
			return true;
		} else if(attribute.length == end - start) {
			for(int i = 0; i < attribute.length; i++) {
				if(attribute[i] != chars[start + i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
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
	
	public int getMaxPathMatches() {
		return maxPathMatches;
	}
	
}
