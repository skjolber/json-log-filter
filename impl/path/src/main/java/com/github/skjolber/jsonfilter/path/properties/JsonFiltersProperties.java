package com.github.skjolber.jsonfilter.path.properties;

import java.util.ArrayList;
import java.util.List;

public class JsonFiltersProperties {

	protected boolean enabled = true;
	
	protected ProcessingProperties requests = new ProcessingProperties(true, true);
	protected ProcessingProperties responses = new ProcessingProperties(false, false);

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

	public void setRequests(ProcessingProperties requests) {
		this.requests = requests;
	}
	
	public void setResponses(ProcessingProperties responses) {
		this.responses = responses;
	}
	
	public ProcessingProperties getRequests() {
		return requests;
	}
	
	public ProcessingProperties getResponses() {
		return responses;
	}
}
