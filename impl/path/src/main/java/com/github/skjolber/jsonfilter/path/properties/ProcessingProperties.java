package com.github.skjolber.jsonfilter.path.properties;

public class ProcessingProperties {

	protected boolean validate;
	protected boolean compact;

	public ProcessingProperties() {
	}
	
	public ProcessingProperties(boolean validate, boolean compact) {
		super();
		this.validate = validate;
		this.compact = compact;
	}

	public boolean isCompact() {
		return compact;
	}
	
	public void setCompact(boolean compact) {
		this.compact = compact;
	}
	
	public void setValidate(boolean validate) {
		this.validate = validate;
	}
	
	public boolean isValidate() {
		return validate;
	}
}
