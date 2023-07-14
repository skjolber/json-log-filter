package com.github.skjolber.jsonfilter.test.cache;

import java.util.List;

/**
 * 
 * A single JSON and all its whitespace variants.
 * 
 */

public class MaxSizeJsonCollection extends MaxSizeJsonItem {

	// pretty-printing variants
	protected final List<MaxSizeJsonItem> prettyPrinted;

	public MaxSizeJsonCollection(String content, int mark, List<MaxSizeJsonItem> prettyPrinted) {
		super(mark, content);
		this.prettyPrinted = prettyPrinted;
	}
	
	public List<MaxSizeJsonItem> getPrettyPrinted() {
		return prettyPrinted;
	}
	
	public MaxSizeJsonItem getPrettyPrinted(int index) {
		return prettyPrinted.get(index);
	}
	
	public int getPrettyPrintedCount() {
		return prettyPrinted.size();
	}
	

	public int getMaxLength() {
		int max = contentAsString.length();
		for (MaxSizeJsonItem item: prettyPrinted) {
			if(item.getContentAsString().length() > max) {
				max = item.getContentAsString().length();
			}
		}
				
		return max;
	}

}
