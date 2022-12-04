package com.github.skjolber.jsonfilter.test.directory;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import com.github.skjolber.jsonfilter.JsonFilter;

/**
 * 
 * Read bean info and get properties which can be directly compared to properties from a properties-file.
 * 
 */

public class JsonFilterPropertiesFactory {

	public static Properties noopProperties;

	static {
		noopProperties = new Properties();
	}
	
	public static boolean isFilterDirectory(Path directory) {
		Path resolve = directory.resolve("filter.properties");
		return Files.exists(resolve);
	}

	private final Set<?> nullableValues;
	private final Set<String> ignorableProperties;

	public JsonFilterPropertiesFactory(List<?> nullableValues, List<String> ignorableProperties) {
		this.nullableValues = new HashSet<>(nullableValues);
		this.ignorableProperties = new HashSet<>(ignorableProperties);
	}

	protected void put(Properties properties, Object invoke, String name) {
		Object normalizeValue = normalizeValue(invoke);
		if(normalizeValue != null) {
			Object reducedValue = reduceValue(normalizeValue);
			if(reducedValue != null) {
				if(reducedValue instanceof Number) {
					properties.put(name, reducedValue.toString());
				} else {
					properties.put(name, reducedValue);					
				}
			}
		}
	}

	protected Object reduceValue(Object normalizeValue) {
		if(nullableValues.contains(normalizeValue)) {
			return null;
		}
		if(normalizeValue instanceof List) {
			List<Object> output = new ArrayList<>(); 

			List<?> input = (List<?>)normalizeValue; 
			for (Object value : input) {
				Object reducedValue = reduceValue(value);
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
			if(integer.intValue() == -1 || integer.intValue() == (Integer.MAX_VALUE - 2) || integer.intValue() == Integer.MAX_VALUE) {
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

	public Properties readDirectoryProperties(Path directory) throws IOException {
		return normalize(readPropertiesFile(directory.resolve("filter.properties")));
	}

	private Properties normalize(Properties properties) {
		
		Properties normalizedProperties = new Properties();
		for (Entry<Object, Object> entry : properties.entrySet()) {
			put(normalizedProperties, entry.getValue(), (String)entry.getKey());
		}
		
		return normalizedProperties;
	}
	
	public Properties readPropertiesFile(Path filter) throws IOException {
		Properties properties = new Properties();
		InputStream fin = Files.newInputStream(filter);
		try {
			properties.load(fin);
		} finally {
			fin.close();
		}
		return properties;
	}
	
	public JsonFilterProperties createInstance(JsonFilter filter) {
		try {
			Properties properties = new Properties();
			BeanInfo beanInfo = Introspector.getBeanInfo(filter.getClass());
			PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
			if(propertyDescriptors == null || propertyDescriptors.length == 1) {
				return new JsonFilterProperties(filter, null);
			}
			for (PropertyDescriptor pd : propertyDescriptors) {
				Method readMethod = pd.getReadMethod();
				if(readMethod != null) {
					Object invoke = readMethod.invoke(filter);
					
					String name = readMethod.getName();
					if(!name.equals("getClass")) {
						String normalize = normalize(name);
						if(!name.equals("getClass") && !ignorableProperties.contains(normalize)) {
							put(properties, invoke, normalize);
						}
					}
				}
			}
			
			return new JsonFilterProperties(filter, properties);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	

	
}
