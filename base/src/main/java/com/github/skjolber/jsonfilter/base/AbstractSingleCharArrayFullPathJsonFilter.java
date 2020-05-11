package com.github.skjolber.jsonfilter.base;

public abstract class AbstractSingleCharArrayFullPathJsonFilter extends AbstractSinglePathJsonFilter {

	protected final char[][] paths;
	
	public AbstractSingleCharArrayFullPathJsonFilter(int maxStringLength, String expression, FilterType type) {
		super(maxStringLength, expression, type);
		
		if(expression.startsWith(AbstractPathJsonFilter.ANY_PREFIX)) {
			throw new IllegalArgumentException("Any element expression not supported");
		}

		this.paths = toCharArray(parse(expression));
		for(int i = 0; i < paths.length; i++) {
			paths[i] = intern(paths[i]);
		}
	}
	
}
