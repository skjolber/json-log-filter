package com.github.skjolber.jsonfilter.spring.autoconfigure.logbook;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.github.skjolber.jsonfilter.JsonFilter;

public class ValidatingJsonFilterProcessor extends ValidatingJsonProcessor {

	protected final JsonFilter filter;

	public ValidatingJsonFilterProcessor(JsonFilter filter, JsonFactory factory, boolean compacting) {
		super(factory, compacting);
		this.filter = filter;
	}

	protected String handleBodyAsString(String bodyAsString) {
		String filtered = filter.process(bodyAsString);
		
		if(filtered != null) {
			if(!filter.isValidating()) {
				// will also compact
				try {
					return validate(filtered);
				} catch (Exception e) {
					// fall through to escape as string
				}
			} else if(compacting && (!filter.isRemovingLinebreaksInStrings() || !filter.isRemovingWhitespace())) {
				return compact(filtered);
			} else {
				return filtered;
			}
		}

		return handleInvalidBodyAsString(bodyAsString, compacting);
	}
	
	protected byte[] handleBodyAsBytes(byte[] bodyAsString) throws IOException {
		byte[] filtered = filter.process(bodyAsString);
		
		if(filtered != null) {
			if(!filter.isValidating()) {
				// will also compact
				try {
					return validate(filtered);
				} catch (Exception e) {
					// fall through to escape as string
				}
			} else if(compacting && (!filter.isRemovingLinebreaksInStrings() || !filter.isRemovingWhitespace())) {
				return compact(filtered);
			} else {
				return filtered;
			}
		}

		return handleInvalidBodyAsBytes(bodyAsString, compacting);
	}

}
