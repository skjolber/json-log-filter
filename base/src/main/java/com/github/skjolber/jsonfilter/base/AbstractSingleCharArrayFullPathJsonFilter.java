package com.github.skjolber.jsonfilter.base;

public abstract class AbstractSingleCharArrayFullPathJsonFilter extends AbstractSinglePathJsonFilter {

	protected final char[][] pathChars;
	protected final byte[][] pathBytes;
	
	public AbstractSingleCharArrayFullPathJsonFilter(int maxStringLength, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
		
		if(hasAnyPrefix(expression)) {
			throw new IllegalArgumentException("Any element expression not supported");
		}

		String[] parsed = parse(expression);
		this.pathChars = toCharArray(parsed);
		this.pathBytes = toByteArray(parsed);
	}

}
