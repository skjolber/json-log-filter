package com.github.skjolber.jsonfilter.test.truth;

import java.util.Objects;

public class JsonFilterResult {

	private final String stringInput;
	private final String stringOutput;
	
	public JsonFilterResult(String stringInput, String stringOutput) {
		this.stringInput = stringInput;
		this.stringOutput = stringOutput;
	}
	
	public String getStringInput() {
		return stringInput;
	}
	
	public String getStringOutput() {
		return stringOutput;
	}

	@Override
	public int hashCode() {
		return Objects.hash(stringInput, stringOutput);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JsonFilterResult other = (JsonFilterResult) obj;
		return Objects.equals(stringInput, other.stringInput) && Objects.equals(stringOutput, other.stringOutput);
	}

	public boolean hasStringOutput() {
		return stringOutput != null;
	}
	
}
