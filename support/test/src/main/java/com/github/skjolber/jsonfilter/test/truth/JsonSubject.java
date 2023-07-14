package com.github.skjolber.jsonfilter.test.truth;

import static com.google.common.truth.Fact.simpleFact;
import static com.google.common.truth.Fact.fact;
import static com.google.common.truth.Truth.assertAbout;

import java.nio.charset.StandardCharsets;

import com.github.skjolber.jsonfilter.test.JsonFilterDirectoryUnitTestCollection;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparator;
import com.github.skjolber.jsonfilter.test.jackson.JsonNormalizer;
import com.github.skjolber.jsonfilter.test.jackson.JsonValidator;
import com.google.common.base.Objects;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
/**
 * Propositions for {@link JsonFilterDirectoryUnitTestCollection} subjects.
 *
 */
public class JsonSubject extends Subject {

	// User-defined entry point
	public static JsonSubject assertThatJson(String result) {
		return assertAbout(JSON_SUBJECT_FACTORY).that(result);
	}
	
	public static JsonSubject assertThatJson(byte[] result) {
		return assertAbout(JSON_SUBJECT_FACTORY).that(new String(result, StandardCharsets.UTF_8));
	}

	public static Factory<JsonSubject, String> jsonFilterResults() {
		return JsonSubject::new;
	}

	// Boiler-plate Subject.Factory for EmployeeSubject
	private static final Subject.Factory<JsonSubject, String> JSON_SUBJECT_FACTORY = JsonSubject::new;	

	protected final String actual;
	protected final String normalized;

	/**
	 * Constructor for use by subclasses. If you want to create an instance of this class itself, call
	 * {@link Subject#check(String, Object...) check(...)}{@code .that(actual)}.
	 */

	private JsonSubject(FailureMetadata metadata, String value) {
		super(metadata, value);
		this.actual = value;
		this.normalized = JsonNormalizer.normalize(value);
	}

	@Override
	public final void isEqualTo(Object other) {
		String otherAsString = toString(other);

		if (Objects.equal(actual, otherAsString)) {
			return;
		}
		
		String otherAsNormalizedString = JsonNormalizer.normalize(otherAsString);
		if (Objects.equal(otherAsNormalizedString, normalized)) {
			return;
		}
		
		String printDiff = JsonComparator.printDiff(normalized, otherAsNormalizedString);
		
		failWithActual(fact("expected to be equal", printDiff));
	}

	private String toString(Object other) {
		String otherAsString;
		if(other instanceof byte[]) {
			otherAsString = new String((byte[])other, StandardCharsets.UTF_8);
		} else if(other instanceof String) {
			otherAsString = (String)other;
		} else if(other instanceof char[]) {
			otherAsString = new String((char[])other);
		} else if(other instanceof StringBuilder) {
			otherAsString = other.toString();
		} else {
			failWithActual(simpleFact("expected to be instance of String, StringBuilder, char[] or byte[]"));
			return null;
		}
		return otherAsString;
	}

	/** Fails if the map is not empty. */
	public final void isEmpty() {
		if (actual.isEmpty()) {
			failWithActual(simpleFact("expected to be empty"));
		}
	}

	/** Fails if the map is empty. */
	public final void isSize(int size) {
		if (actual.length() != size) {
			failWithoutActual(simpleFact("expected size " + size + ", was " + actual.length()));
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

	public final void isEqualEventsTo(Object other) {
		String otherAsString = toString(other);

		if(JsonComparator.isEventsEqual(actual, otherAsString)) {
			return;
		}
		
		String otherAsNormalizedString = JsonNormalizer.normalize(otherAsString);
		if(JsonComparator.isEventsEqual(normalized, otherAsNormalizedString)) {
			return;
		}
		
		String printDiff = JsonComparator.printDiff(normalized, otherAsNormalizedString);
		
		failWithActual(fact("expected to be equal events", printDiff));
	}

}
