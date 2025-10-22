package com.github.skjolber.jsonfilter.jmh.path;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractFieldNames {
	
	public static List<String> extract(String json) {
		List<String> result = new ArrayList<>();
		
		String regex = "\".+\":"; // Matches one or more digits
	    Pattern pattern = Pattern.compile(regex);
	    Matcher matcher = pattern.matcher(json);
	    
	    while (matcher.find()) {
	        result.add(json.substring(matcher.start() + 1, matcher.end() - 2));
	    }
		return result;
	}

}
