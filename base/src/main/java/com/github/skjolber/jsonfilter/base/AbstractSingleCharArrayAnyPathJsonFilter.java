package com.github.skjolber.jsonfilter.base;

public abstract class AbstractSingleCharArrayAnyPathJsonFilter extends AbstractSinglePathJsonFilter {

	protected final char[] path;
	
	public AbstractSingleCharArrayAnyPathJsonFilter(int maxStringLength, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
		
		if(!expression.startsWith(AbstractPathJsonFilter.ANY_PREFIX)) {
			throw new IllegalArgumentException("Full element path expression not supported");
		}
		
		this.path = expression.substring(2).toCharArray();
	}
	
}
