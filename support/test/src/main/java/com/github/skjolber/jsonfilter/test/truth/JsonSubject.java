package com.github.skjolber.jsonfilter.test.truth;

import static com.google.common.truth.Fact.simpleFact;
import static com.google.common.truth.Truth.assertAbout;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import com.github.skjolber.jsonfilter.test.JsonFilterResult;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparator;
import com.github.skjolber.jsonfilter.test.jackson.JsonValidator;
import com.google.common.base.Objects;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
/**
 * Propositions for {@link JsonFilterResult} subjects.
 *
 */
public class JsonSubject extends Subject {

	// User-defined entry point
	public static JsonSubject assertThat(@NullableDecl String result) {
		return assertAbout(JSON_SUBJECT_FACTORY).that(result);
	}
	
	public static JsonSubject assertThat(@NullableDecl byte[] result) {
		return assertAbout(JSON_SUBJECT_FACTORY).that(new String(result, StandardCharsets.UTF_8));
	}

	public static Factory<JsonSubject, String> jsonFilterResults() {
		return JsonSubject::new;
	}	

	// Static method for getting the subject factory (for use with assertAbout())
	public static Subject.Factory<JsonSubject, String> employees() {
		return JSON_SUBJECT_FACTORY;
	}

	// Boiler-plate Subject.Factory for EmployeeSubject
	private static final Subject.Factory<JsonSubject, String> JSON_SUBJECT_FACTORY = JsonSubject::new;	

	protected final String actual;

	/**
	 * Constructor for use by subclasses. If you want to create an instance of this class itself, call
	 * {@link Subject#check(String, Object...) check(...)}{@code .that(actual)}.
	 */

	private JsonSubject(FailureMetadata metadata, @NullableDecl String value) {
		super(metadata, value);
		if(isHighSurrogate(value)) {
			String surrogates = filterSurrogates(value);

			
			
		} else {
			this.actual = value;
		}
	}
	
	public static String filterSurrogates(String result) {
		// ...TRUNCATED BY 

		Pattern patt = Pattern.compile("(TRUNCATED BY )([0-9]+)");
		Matcher m = patt.matcher(result);
		StringBuffer sb = new StringBuffer(result.length());
		while (m.find()) {
			String text = m.group(1);
			m.appendReplacement(sb, text + "XX");
		}
		m.appendTail(sb);
		return sb.toString();
	}

	private static boolean isHighSurrogate(String from) {
		for(int i = 0; i < from.length(); i++) {
			if(Character.isHighSurrogate(from.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public final void isEqualTo(@NullableDecl Object other) {
		String otherAsString;
		if(other instanceof byte[]) {
			otherAsString = new String((byte[])other, StandardCharsets.UTF_8);
		} else if(other instanceof String) {
			otherAsString = (String)other;
		} else if(other instanceof char[]) {
			otherAsString = new String((char[])other);
		} else {
			failWithActual(simpleFact("expected to be instance of String, char[] or byte[]"));
			return;
		}

		boolean otherHighSurrogate = isHighSurrogate(otherAsString);
		
		if(otherHighSurrogate || highSurrogate) {
			String actualFiltered = filterSurrogates(actual);
			String otherFiltered = filterSurrogates(otherAsString);
			
			if (Objects.equal(actualFiltered, otherFiltered)) {
				return;
			}
		} else {
			if (Objects.equal(actual, other)) {
				return;
			}
		}
		
		failWithActual(simpleFact("expected to be equal"));
	}

	/** Fails if the map is not empty. */
	public final void isEmpty() {
		if (!actual.isEmpty()) {
			failWithActual(simpleFact("expected to be empty"));
		}
	}

	/** Fails if the map is empty. */
	public final void isNotEmpty() {
		if (actual.isEmpty()) {
			failWithoutActual(simpleFact("expected not to be empty"));
		}
	}

	/** Fails JSON is not well formed
	 * 
	 * @return this  
	 */
	public final JsonSubject isWellformed() {
		if (!JsonValidator.isWellformed(actual)) {
			failWithoutActual(simpleFact("expected wellformed"));
		}
		
		return this;
	}

	public final void isEqualEventsTo(@NullableDecl Object other) {
		String otherAsString;
		if(other instanceof byte[]) {
			otherAsString = new String((byte[])other, StandardCharsets.UTF_8);
		} else if(other instanceof String) {
			otherAsString = (String)other;
		} else if(other instanceof char[]) {
			otherAsString = new String((char[])other);
		} else {
			failWithActual(simpleFact("expected to be instance of String, char[] or byte[]"));
			return;
		}

		boolean otherHighSurrogate = isHighSurrogate(otherAsString);
		
		if(otherHighSurrogate || highSurrogate) {
			String actualFiltered = filterSurrogates(actual);
			String otherFiltered = filterSurrogates(otherAsString);
			
			if (JsonComparator.isSameEvents(actualFiltered, otherFiltered)) {
				return;
			}
		} else {
			if (JsonComparator.isSameEvents(actual, otherAsString)) {
				return;
			}
		}
		
		failWithActual(simpleFact("expected to be equal"));
	}


}
