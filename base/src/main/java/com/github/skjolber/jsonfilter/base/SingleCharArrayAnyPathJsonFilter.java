package com.github.skjolber.jsonfilter.base;

public abstract class SingleCharArrayAnyPathJsonFilter extends AbstractSinglePathJsonFilter {

	protected final char[] path;
	
	public SingleCharArrayAnyPathJsonFilter(int maxStringLength, String expression, FilterType type) {
		super(maxStringLength, expression, type);
		
		if(!expression.startsWith(AbstractPathJsonFilter.ANY_PREFIX)) {
			throw new IllegalArgumentException("Full element path expression not supported");
		}
		
		this.path = expression.substring(2).toCharArray();
	}
	
}
