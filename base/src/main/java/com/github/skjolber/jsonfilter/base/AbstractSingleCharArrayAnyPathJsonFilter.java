package com.github.skjolber.jsonfilter.base;

import java.nio.charset.StandardCharsets;

public abstract class AbstractSingleCharArrayAnyPathJsonFilter extends AbstractSinglePathJsonFilter {

	protected final char[] pathChars;
	protected final byte[] pathBytes;
	
	public AbstractSingleCharArrayAnyPathJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
		
		if(!hasAnyPrefix(expression)) {
			throw new IllegalArgumentException("Full element path expression not supported");
		}
		
		String key = expression.substring(2);
		this.pathChars = intern(key.toCharArray());
		this.pathBytes = intern(key.getBytes(StandardCharsets.UTF_8));
	}
	
}
