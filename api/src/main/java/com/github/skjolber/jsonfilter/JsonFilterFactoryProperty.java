package com.github.skjolber.jsonfilter;

public enum JsonFilterFactoryProperty {
	MAX_STRING_LENGTH("com.github.skjolber.jsonfilter.maxStringLength"),
	PRUNE("com.github.skjolber.jsonfilter.prune"),
	ANONYMIZE("com.github.skjolber.jsonfilter.anonymize"),
	MAX_PATH_MATCHES("com.github.skjolber.jsonfilter.maxPathMatches"),
	PRUNE_MESSAGE("com.github.skjolber.jsonfilter.pruneMessage"),
	ANON_MESSAGE("com.github.skjolber.jsonfilter.anonymizeMessage"),
	TRUNCATE_MESSAGE("com.github.skjolber.jsonfilter.truncateMessage"),
	MAX_SIZE("com.github.skjolber.jsonfilter.maxSize"),
	REMOVE_WHITESPACE("com.github.skjolber.jsonfilter.removeWhitespace")
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