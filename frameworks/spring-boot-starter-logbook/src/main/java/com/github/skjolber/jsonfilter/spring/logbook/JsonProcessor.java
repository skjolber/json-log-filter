package com.github.skjolber.jsonfilter.spring.logbook;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.io.JsonStringEncoder;

public abstract class JsonProcessor {

	protected String handleInvalidBodyAsString(String bodyAsString, boolean compact) {
		if(compact) {
			bodyAsString = compact(bodyAsString);
		}

		// escape as string
		return escapeAsJsonTextValue(bodyAsString);
	}
	
	protected String escapeAsJsonTextValue(String filtered) {
		StringBuilder output = new StringBuilder();
		output.append('"');
		JsonStringEncoder.getInstance().quoteAsString(filtered, output);
		output.append('"');
		return output.toString();
	}
	
	protected byte[] escapeAsJsonTextValue(byte[] filtered) {
		// TODO slow, but usually not on critical path
		String escaped = escapeAsJsonTextValue(new String(filtered, StandardCharsets.UTF_8));
		
		return escaped.getBytes(StandardCharsets.UTF_8);
	}

	protected byte[] handleInvalidBodyAsBytes(byte[] bodyAsString, boolean compact) {
		// escape as string
		if(compact) {
			bodyAsString = compact(bodyAsString);
		}

		return escapeAsJsonTextValue(bodyAsString);
	}
    
    protected byte[] compact(byte[] json) {
    	for(int i = 0; i < json.length; i++) {
    		if(json[i] == '\n') {
    			String escaped = compact(new String(json, StandardCharsets.UTF_8));

    			return escaped.getBytes(StandardCharsets.UTF_8);
    		}
    	}
    	return json;
    }
    
    protected String compact(String json) {
        return json.replace("\n", "");
    }
    

	public abstract byte[] processBody(byte[] body) throws IOException;

	public abstract  String processBody(String string) throws IOException;
    
}
