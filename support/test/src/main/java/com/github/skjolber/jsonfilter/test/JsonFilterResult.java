package com.github.skjolber.jsonfilter.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;

public class JsonFilterResult {

	private List<JsonFilterOutputDirectory> filtered = new ArrayList<>();
	private List<JsonFilterOutputDirectory> passthrough = new ArrayList<>();

	private List<Map<File, DefaultJsonFilterMetrics[]>> metrics = new ArrayList<>();

	public JsonFilterResult() {
	}
	
	public boolean addFiltered(JsonFilterOutputDirectory e) {
		return filtered.add(e);
	}

	public boolean addPassthrough(JsonFilterOutputDirectory e) {
		return passthrough.add(e);
	}

	public List<JsonFilterOutputDirectory> getFiltered() {
		return filtered;
	}
	
	public List<JsonFilterOutputDirectory> getPassthrough() {
		return passthrough;
	}

	public boolean hasPropertyKey(String key) {
		for (JsonFilterOutputDirectory jsonFilterOutputDirectory : filtered) {
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
		
		for (JsonFilterOutputDirectory jsonFilterOutputDirectory : filtered) {
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

		for (JsonFilterOutputDirectory jsonFilterOutputDirectory : filtered) {
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
	
	public void add(Map<File, DefaultJsonFilterMetrics[]> m) {
		metrics.add(m);
	}
	
	private boolean filter(Function<DefaultJsonFilterMetrics, Integer> object) {
		for (Map<File, DefaultJsonFilterMetrics[]> map : metrics) {
			
			for (Entry<File, DefaultJsonFilterMetrics[]> entry : map.entrySet()) {
				for (DefaultJsonFilterMetrics defaultJsonFilterMetrics : entry.getValue()) {
					if(object.apply(defaultJsonFilterMetrics) != 0) {
						return true;
					}
				}
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
