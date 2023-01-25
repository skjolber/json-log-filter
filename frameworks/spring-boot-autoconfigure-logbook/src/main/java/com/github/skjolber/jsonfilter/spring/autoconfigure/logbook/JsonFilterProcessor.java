package com.github.skjolber.jsonfilter.spring.autoconfigure.logbook;

import java.io.IOException;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.path.properties.WhitespaceStrategy;

/**
 * Compacting JSON processor
 */

public class JsonFilterProcessor extends CompactingJsonProcessor {

	protected final JsonFilter filter;
	protected final WhitespaceStrategy whitespaceStrategy;

	public JsonFilterProcessor(JsonFilter filter, WhitespaceStrategy whitespaceStrategy) {
		this.filter = filter;
		this.whitespaceStrategy = whitespaceStrategy;
	}

	@Override
	public String processBody(String body) throws IOException {
		String filtered = filter.process(body);
		
		if(filtered != null) {
			if(whitespaceStrategy != WhitespaceStrategy.NEVER && (!filter.isRemovingLinebreaksInStrings() || !filter.isRemovingWhitespace())) {
				return compact(filtered);
			} else {
				return filtered;
			}
		}

		return handleInvalidBodyAsString(body, whitespaceStrategy);
	}
	
	@Override
	public byte[] processBody(byte[] body) throws IOException {
		byte[] filtered = filter.process(body);
		
		if(filtered != null) {
			if(whitespaceStrategy != WhitespaceStrategy.NEVER && (!filter.isRemovingLinebreaksInStrings() || !filter.isRemovingWhitespace())) {
				return compact(filtered);
			} else {
				return filtered;
			}
		}

		return handleInvalidBodyAsBytes(body, whitespaceStrategy);
	}

}
