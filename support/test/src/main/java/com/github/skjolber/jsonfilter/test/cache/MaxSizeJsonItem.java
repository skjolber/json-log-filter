package com.github.skjolber.jsonfilter.test.cache;

import java.util.Objects;

public class MaxSizeJsonItem {

	protected final int mark;
	protected final String contentAsString;
	
	public MaxSizeJsonItem(int mark, String content) {
		this.contentAsString = content;
		this.mark = mark;
	}

	public int getMark() {
		return mark;
	}

	public int nextCodePoint(int start) {
		int codepoint = contentAsString.codePointAt(start);
		return start + Character.charCount(codepoint);
	}
	
	public String getContentAsString() {
		return contentAsString;
	}

	@Override
	public int hashCode() {
		return Objects.hash(contentAsString, mark);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MaxSizeJsonItem other = (MaxSizeJsonItem) obj;
		return Objects.equals(contentAsString, other.contentAsString) && mark == other.mark;
	}

	
}
