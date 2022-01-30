package com.github.skjolber.jsonfilter.path.properties;

import java.util.ArrayList;
import java.util.List;

public class JsonFilterProperties {

	protected boolean enabled = true;

	protected boolean validate = false;

	protected int maxStringLength = -1;
	protected int maxPathMatches = -1;
	
	protected List<String> anonymizes = new ArrayList<>();
	protected List<String> prunes = new ArrayList<>();
	
	public boolean isValidate() {
		return validate;
	}
	public void setValidate(boolean validate) {
		this.validate = validate;
	}

	public int getMaxStringLength() {
		return maxStringLength;
	}
	public void setMaxStringLength(int maxStringLength) {
		this.maxStringLength = maxStringLength;
	}
	public int getMaxPathMatches() {
		return maxPathMatches;
	}
	public void setMaxPathMatches(int maxPathMatches) {
		this.maxPathMatches = maxPathMatches;
	}
	public List<String> getAnonymizes() {
		return anonymizes;
	}
	public void setAnonymizes(List<String> anonymizeFilters) {
		this.anonymizes = anonymizeFilters;
	}
	public List<String> getPrunes() {
		return prunes;
	}
	public void setPrunes(List<String> pruneFilters) {
		this.prunes = pruneFilters;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
}
