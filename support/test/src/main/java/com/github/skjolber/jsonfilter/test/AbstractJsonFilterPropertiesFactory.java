package com.github.skjolber.jsonfilter.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * 
 * Read bean info and get properties which can be directly compared to properties from a properties-file.
 * 
 */

public abstract class AbstractJsonFilterPropertiesFactory {

	protected static final Properties noopProperties;

	static {
		noopProperties = new Properties();
	}
	
	public static boolean isFilterDirectory(File directory) {
		return new File(directory, "filter.properties").exists();
	}
	
	private final List<?> nullable;
	
	public AbstractJsonFilterPropertiesFactory(List<?> nullable) {
		this.nullable = nullable;
	}

	protected void put(Properties properties, Object invoke, String name) {
		Object normalizeValue = normalizeValue(invoke);
		if(normalizeValue != null) {
			Object reducedValue = reduceValue(normalizeValue, nullable);
			if(reducedValue != null) {
				if(reducedValue instanceof Number) {
					properties.put(name, reducedValue.toString());
				} else {
					properties.put(name, reducedValue);					
				}
			}
		}
	}

	protected static Object reduceValue(Object normalizeValue, List<?> nullable) {
		if(nullable.contains(normalizeValue)) {
			return null;
		}
		if(normalizeValue instanceof List) {
			List<Object> output = new ArrayList<>(); 

			List<?> input = (List<?>)normalizeValue; 
			for (Object value : input) {
				Object reducedValue = reduceValue(value, nullable);
				if(reducedValue != null) {
					output.add(reducedValue);
				}
			}
			if(!output.isEmpty()) {
				if(output.size() == 1) {
					return output.get(0);
				}
				return output;
			}
			return null;
		}
		return normalizeValue;
	}

	@SuppressWarnings("unchecked")
	protected static Object normalizeValue(Object invoke) {
		// make sure the object can respond to equals
		if(invoke instanceof String[]) {
			String[] strings = (String[])invoke;
			if(strings.length > 0) {
				return normalizeValue(Arrays.asList(strings));
			}
			return null;
		} else if(invoke instanceof List) {
			List<Object> list = (List<Object>)invoke;
			if(!list.isEmpty()) {
				if(list.size() == 1) {
					return normalizeValue(list.get(0));
				}
				
				for(int i = 0; i < list.size(); i++) {
					Object value = normalizeValue(list.get(i));
					if(value == null) {
						list.remove(i);
						i--;
					}
				}
				
				return list;
			}
			return null;
		} else if(invoke instanceof Integer) {
			Integer integer = (Integer)invoke;
			if(integer.intValue() == -1 || integer.intValue() == (Integer.MAX_VALUE - 2)) {
				return null;
			}
			return integer;
		} else if(invoke instanceof Boolean) {
			Boolean integer = (Boolean)invoke;
			if(!integer.booleanValue()) {
				return null;
			}
			return integer;
		} else if(invoke instanceof String) {
			String string = (String)invoke;
			if(string.equals(Boolean.TRUE.toString())) {
				return Boolean.TRUE;
			} else if(string.equals(Boolean.FALSE.toString())) {
				return Boolean.FALSE;
			}
			
			try {
				return normalizeValue(Integer.parseInt(string));
			} catch(NumberFormatException e) {
				// ignore
			}
			
			if(string.contains(",")) {
				return normalizeValue(string.split(","));
			}
					
			return string;
		}
		
		return invoke;
	}

	protected static String normalize(String name) {
		if(name.startsWith("get")) {
			return Character.toLowerCase(name.charAt(3)) + name.substring(4);
		} else if(name.startsWith("is")) {
			return Character.toLowerCase(name.charAt(2)) + name.substring(3);
		}
		throw new IllegalArgumentException();
	}

}
