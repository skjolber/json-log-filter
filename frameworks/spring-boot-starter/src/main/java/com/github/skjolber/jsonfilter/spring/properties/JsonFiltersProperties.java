package com.github.skjolber.jsonfilter.spring.properties;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jsonfilter")
public class JsonFiltersProperties {

	protected boolean enabled = true;
	
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
	
	
}
