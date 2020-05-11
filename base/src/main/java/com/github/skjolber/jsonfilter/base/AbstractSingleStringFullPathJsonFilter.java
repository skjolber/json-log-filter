package com.github.skjolber.jsonfilter.base;

public abstract class AbstractSingleStringFullPathJsonFilter extends AbstractSinglePathJsonFilter {

	protected final String[] paths;
	
	public AbstractSingleStringFullPathJsonFilter(int maxStringLength, String expression, FilterType type) {
		super(maxStringLength, expression, type);
		
		if(expression.startsWith(AbstractPathJsonFilter.ANY_PREFIX)) {
			throw new IllegalArgumentException("Any element expression not supported");
		}

		this.paths = parse(expression);
	}
	
	
}
