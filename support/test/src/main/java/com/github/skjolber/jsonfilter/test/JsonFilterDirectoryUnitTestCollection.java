package com.github.skjolber.jsonfilter.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

import com.github.skjolber.jsonfilter.test.directory.JsonFilterDirectoryUnitTest;

public class JsonFilterDirectoryUnitTestCollection {

	private List<JsonFilterDirectoryUnitTest> filtered = new ArrayList<>();
	private List<JsonFilterDirectoryUnitTest> passthrough = new ArrayList<>();

	private List<DefaultJsonFilterMetrics> metrics = new ArrayList<>();

	public JsonFilterDirectoryUnitTestCollection() {
	}
	
	public boolean addFiltered(JsonFilterDirectoryUnitTest e) {
		return filtered.add(e);
	}

	public boolean addPassthrough(JsonFilterDirectoryUnitTest e) {
		return passthrough.add(e);
	}

	public List<JsonFilterDirectoryUnitTest> getFiltered() {
		return filtered;
	}
	
	public List<JsonFilterDirectoryUnitTest> getPassthrough() {
		return passthrough;
	}

	public boolean hasPropertyKey(String key) {
		for (JsonFilterDirectoryUnitTest jsonFilterOutputDirectory : filtered) {
			Properties properties = jsonFilterOutputDirectory.getProperties();
			
			String object = trimNull(properties.getProperty(key));
			if(object != null) {
				return false;
			}
		}
		return true;
	}

	public boolean hasPropertyKeyValue(String key, Object value) {
		if(value instanceof String) {
			value = trimNull((String)value);
		}

		if(filtered.isEmpty()) {
			return false;
		}
		
		for (JsonFilterDirectoryUnitTest jsonFilterOutputDirectory : filtered) {
			Properties properties = jsonFilterOutputDirectory.getProperties();
			Object object = properties.get(key);
			
			if(!Objects.equals(object, value)) {
				return false;
			}
		}
		
		return true;
	}	

	public boolean hasPropertyKeyValues(String ... keyValues) {
		if(filtered.isEmpty()) {
			return false;
		}

		for (JsonFilterDirectoryUnitTest jsonFilterOutputDirectory : filtered) {
			Properties properties = jsonFilterOutputDirectory.getProperties();
			
			for(int i = 0; i < keyValues.length; i+=2) {
				Object propertyValue = properties.get(keyValues[i]);
				if(!Objects.equals(propertyValue, trimNull(keyValues[i+1]))) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	private String trimNull(String string) {
		if(string == null) {
			return string;
		}
		string = string.trim();
		if(string.isEmpty()) {
			return null;
		}
		return string;
	}

	public boolean isEmpty() {
		return filtered.isEmpty();
	}
	
	public int size() {
		return filtered.size();
	}
	
	public boolean hasPassthrough() {
		return !passthrough.isEmpty();
	}
	
	public void add(DefaultJsonFilterMetrics m) {
		metrics.add(m);
	}
	
	private boolean filter(Function<DefaultJsonFilterMetrics, Integer> object) {
		for (DefaultJsonFilterMetrics defaultJsonFilterMetrics : metrics) {
			if(object.apply(defaultJsonFilterMetrics) != 0) {
				return true;
			}
		}
		return false;
	}

	public boolean hasMaxStringLengthMetrics() {
		return filter(DefaultJsonFilterMetrics::getMaxStringLength);
	}
	
	public boolean hasMaxSizeMetrics() {
		return filter(DefaultJsonFilterMetrics::getMaxSize);
	}
	
	public boolean hasPruneMetrics() {
		return filter(DefaultJsonFilterMetrics::getPrune);
	}

	public boolean hasAnonymizeMetrics() {
		return filter(DefaultJsonFilterMetrics::getAnonymize);
	}

	public boolean hasInputSizeMetrics() {
		return filter(DefaultJsonFilterMetrics::getInputSize);
	}

	public boolean hasOutputSizeMetrics() {
		return filter(DefaultJsonFilterMetrics::getOutputSize);
	}

	
}
