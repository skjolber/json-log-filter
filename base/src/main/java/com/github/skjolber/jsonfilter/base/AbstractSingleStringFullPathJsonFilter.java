package com.github.skjolber.jsonfilter.base;

public abstract class AbstractSingleStringFullPathJsonFilter extends AbstractSinglePathJsonFilter {

	protected final String[] paths;
	
	public AbstractSingleStringFullPathJsonFilter(int maxStringLength, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
		
		if(hasAnyPrefix(expression)) {
			throw new IllegalArgumentException("Any element expression not supported");
		}

		this.paths = parse(expression);
	}
	
	
}
