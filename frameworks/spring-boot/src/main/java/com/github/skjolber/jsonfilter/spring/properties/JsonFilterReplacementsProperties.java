package com.github.skjolber.jsonfilter.spring.properties;

public class JsonFilterReplacementsProperties {

	protected String prune;
	protected String anonymize;
	protected String truncate;

	public String getPrune() {
		return prune;
	}

	public void setPrune(String prune) {
		this.prune = prune;
	}

	public String getAnonymize() {
		return anonymize;
	}

	public void setAnonymize(String anonymize) {
		this.anonymize = anonymize;
	}

	public String getTruncate() {
		return truncate;
	}

	public void setTruncate(String truncate) {
		this.truncate = truncate;
	}
	
	public boolean hasPrune() {
		return prune != null;
	}

	public boolean hasAnonymize() {
		return anonymize != null;
	}

	public boolean hasTruncate() {
		return truncate != null;
	}
}
