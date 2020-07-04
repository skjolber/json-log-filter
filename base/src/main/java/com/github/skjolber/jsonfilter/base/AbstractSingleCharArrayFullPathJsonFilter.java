package com.github.skjolber.jsonfilter.base;

public abstract class AbstractSingleCharArrayFullPathJsonFilter extends AbstractSinglePathJsonFilter {

	protected final char[][] paths;
	
	public AbstractSingleCharArrayFullPathJsonFilter(int maxStringLength, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
		
		if(hasAnyPrefix(expression)) {
			throw new IllegalArgumentException("Any element expression not supported");
		}

		this.paths = toCharArray(parse(expression));
		for(int i = 0; i < paths.length; i++) {
			paths[i] = intern(paths[i]);
		}
	}

}
