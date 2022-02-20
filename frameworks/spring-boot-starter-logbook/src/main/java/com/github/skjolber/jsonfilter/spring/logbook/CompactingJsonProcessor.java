package com.github.skjolber.jsonfilter.spring.logbook;

import java.io.IOException;

/**
 * Compacting JSON processor
 */

public class CompactingJsonProcessor extends JsonProcessor {

	@Override
	public byte[] processBody(byte[] body) throws IOException {
		if(body != null) {
			return compact(body);
		}
		return body;
	}
	
	@Override
	public String processBody(String body) throws IOException {
		if(body != null) {
			return compact(body);
		}
		return body;
	}

}
