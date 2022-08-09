package com.github.skjolber.jsonfilter.spring.autoconfigure.logbook;

import java.io.IOException;

import com.github.skjolber.jsonfilter.JsonFilter;

/**
 * Compacting JSON processor
 */

public class JsonFilterProcessor extends CompactingJsonProcessor {

	protected final JsonFilter filter;
	protected final boolean compacting;

	public JsonFilterProcessor(JsonFilter filter, boolean compacting) {
		this.filter = filter;
		this.compacting = compacting;
	}

	@Override
	public String processBody(String body) throws IOException {
		String filtered = filter.process(body);
		
		if(filtered != null) {
			if(compacting && !filter.isRemovingLinebreaks()) {
				return compact(filtered);
			} else {
				return filtered;
			}
		}

		return handleInvalidBodyAsString(body, compacting);
	}
	
	@Override
	public byte[] processBody(byte[] body) throws IOException {
		byte[] filtered = filter.process(body);
		
		if(filtered != null) {
			if(compacting && !filter.isRemovingLinebreaks()) {
				return compact(filtered);
			} else {
				return filtered;
			}
		}

		return handleInvalidBodyAsBytes(body, compacting);
	}

}
