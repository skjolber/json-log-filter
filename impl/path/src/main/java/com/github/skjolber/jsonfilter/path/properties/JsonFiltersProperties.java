package com.github.skjolber.jsonfilter.path.properties;

import java.util.ArrayList;
import java.util.List;

public class JsonFiltersProperties {

	protected boolean enabled = true;
	
	protected boolean validateRequests;
	protected boolean validateResponses;

	protected List<JsonFilterPathProperties> paths = new ArrayList<>();

	protected JsonFilterReplacementsProperties replacements = new JsonFilterReplacementsProperties();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public List<JsonFilterPathProperties> getPaths() {
		return paths;
	}

	public void setPaths(List<JsonFilterPathProperties> filters) {
		this.paths = filters;
	}

	public JsonFilterReplacementsProperties getReplacements() {
		return replacements;
	}

	public void setReplacements(JsonFilterReplacementsProperties replacements) {
		this.replacements = replacements;
	}

	public boolean isValidateRequests() {
		return validateRequests;
	}

	public void setValidateRequests(boolean validateRequests) {
		this.validateRequests = validateRequests;
	}

	public boolean isValidateResponses() {
		return validateResponses;
	}

	public void setValidateResponses(boolean validateResponses) {
		this.validateResponses = validateResponses;
	}
	
	
}
