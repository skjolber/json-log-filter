package com.github.skjolber.jsonfilter.test;

import java.util.Properties;

import com.github.skjolber.jsonfilter.JsonFilter;


public class JsonFilterProperties {

	private Properties properties;
	private JsonFilter filter;
	
	public JsonFilterProperties(JsonFilter filter, Properties properties) {
		this.filter = filter;
		this.properties = properties;
	}

	public JsonFilter getJsonFilter() {
		return filter;
	}
	
	public Properties getProperties() {
		return properties;
	}

	public boolean matches(Properties properties) {
		return this.properties.equals(properties);
	}
	
	public boolean isNoop() {
		return properties == null || properties.isEmpty();
	}
	
	public void setProperties(Properties properties) {
		this.properties = properties;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((properties == null) ? 0 : properties.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JsonFilterProperties other = (JsonFilterProperties) obj;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		return true;
	}
	
}
