package com.github.skjolber.jsonfilter.test;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

import com.github.skjolber.jsonfilter.JsonFilter;

/**
 * 
 * Read bean info and get properties which can be directly compared to properties from a properties-file.
 * 
 */

public class JsonFilterPropertiesFactory extends AbstractJsonFilterPropertiesFactory {

	public JsonFilterPropertiesFactory(List<?> nullable) {
		super(nullable);
	}

	public static Properties noopProperties;

	static {
		noopProperties = new Properties();
	}
	
	public static boolean isFilterDirectory(File directory) {
		return new File(directory, "filter.properties").exists();
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
						put(properties, invoke, normalize(name));
					}
				}
			}
			
			properties.remove("removingWhitespace");
			
			return new JsonFilterProperties(filter, properties);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

}
