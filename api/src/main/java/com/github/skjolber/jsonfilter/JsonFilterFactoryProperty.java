package com.github.skjolber.jsonfilter;

public enum JsonFilterFactoryProperty {
	MAX_STRING_LENGTH("com.skjolberg.jsonfilter.maxStringLength"),
	PRUNE("com.skjolberg.jsonfilter.prune"),
	ANONYMIZE("com.skjolberg.jsonfilter.anonymize"),
	MAX_PATH_MATCHES("com.skjolberg.jsonfilter.maxPathMatches"),
	PRUNE_MESSAGE("com.skjolberg.jsonfilter.pruneMessage"),
	ANON_MESSAGE("com.skjolberg.jsonfilter.anonymizeMessage"),
	TRUNCATE_MESSAGE("com.skjolberg.jsonfilter.truncateMessage")
	;
	
	private final String name;
	
	private JsonFilterFactoryProperty(String name) {
		this.name = name;
	}

	public String getPropertyName() {
		return name;
	}
	
	public static JsonFilterFactoryProperty parse(String key) {
		for (JsonFilterFactoryProperty p : values()) {
			if(key.equals(p.getPropertyName())) {
				return p;
			}
		}
		return null;
	}
}