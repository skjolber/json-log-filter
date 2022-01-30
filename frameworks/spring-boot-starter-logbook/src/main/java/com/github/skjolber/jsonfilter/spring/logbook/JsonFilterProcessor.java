package com.github.skjolber.jsonfilter.spring.logbook;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.github.skjolber.jsonfilter.JsonFilter;

public class JsonFilterProcessor extends JsonProcessor {

	protected final JsonFilter filter;

	public JsonFilterProcessor(JsonFilter filter, boolean compact, boolean validate, JsonFactory factory) {
		super(compact, validate, factory);
		this.filter = filter;
	}

	protected String handleBodyAsString(String bodyAsString) {
		String filtered = filter.process(bodyAsString);
		
		if(filtered != null) {
			if(validate && !filter.isValidating()) {
				// will also compact
				try {
					return validate(filtered);
				} catch (Exception e) {
					// fall through to escape as string
				}
			} else if(compact && !filter.isCompacting()) {
				return compact(filtered);
			} else {
				return filtered;
			}
		}

		return handleInvalidBodyAsString(bodyAsString);
	}
	
	protected byte[] handleBodyAsBytes(byte[] bodyAsString) throws IOException {
		byte[] filtered = filter.process(bodyAsString);
		
		if(filtered != null) {
			if(validate && !filter.isValidating()) {
				// will also compact
				try {
					return validate(filtered);
				} catch (Exception e) {
					// fall through to escape as string
				}
			} else if(compact && !filter.isCompacting()) {
				return compact(filtered);
			} else {
				return filtered;
			}
		}

		return handleInvalidBodyAsBytes(bodyAsString);
	}

}
