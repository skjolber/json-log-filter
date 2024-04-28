package com.github.skjolber.jsonfilter.path.properties;

public class ProcessingProperties {

	protected boolean validate;
	protected WhitespaceStrategy whitespaceStrategy;
	protected int maxSize = -1;

	public ProcessingProperties() {
	}
	
	public ProcessingProperties(boolean validate, WhitespaceStrategy strategy) {
		super();
		this.validate = validate;
		this.whitespaceStrategy = strategy;
	}

	public WhitespaceStrategy getWhitespaceStrategy() {
		return whitespaceStrategy;
	}
	
	public void setWhitespaceStrategy(WhitespaceStrategy strategy) {
		this.whitespaceStrategy = strategy;
	}
	
	public void setValidate(boolean validate) {
		this.validate = validate;
	}
	
	public boolean isValidate() {
		return validate;
	}
	
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}
	
	public int getMaxSize() {
		return maxSize;
	}

	public boolean hasMaxSize() {
		return maxSize != -1;
	}

	public boolean hasWhitespaceStrategy() {
		return whitespaceStrategy != null;
	}
}
