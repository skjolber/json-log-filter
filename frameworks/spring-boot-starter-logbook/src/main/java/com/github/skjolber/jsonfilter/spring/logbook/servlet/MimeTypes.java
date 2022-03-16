package com.github.skjolber.jsonfilter.spring.logbook.servlet;

import java.util.Optional;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

final class MimeTypes {

	private MimeTypes() {

	}

	static Optional<MimeType> parse(final String mimeType) {
		try {
			return Optional.of(new MimeType(mimeType));
		} catch (final MimeTypeParseException e) {
			return Optional.empty();
		}
	}

}
