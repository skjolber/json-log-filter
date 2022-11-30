package com.github.skjolber.jsonfilter.test.truth;

import static com.google.common.truth.Fact.simpleFact;
import static com.google.common.truth.Truth.assertAbout;

import java.nio.charset.StandardCharsets;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

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
	public static JsonSubject assertThatJson(@NullableDecl String result) {
		return assertAbout(JSON_SUBJECT_FACTORY).that(result);
	}
	
	public static JsonSubject assertThatJson(@NullableDecl byte[] result) {
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
	protected final String normalized;

	/**
	 * Constructor for use by subclasses. If you want to create an instance of this class itself, call
	 * {@link Subject#check(String, Object...) check(...)}{@code .that(actual)}.
	 */

	private JsonSubject(FailureMetadata metadata, @NullableDecl String value) {
		super(metadata, value);
		this.actual = value;
		this.normalized = JsonNormalizer.normalize(value);
	}

	@Override
	public final void isEqualTo(@NullableDecl Object other) {
		String otherAsString = toString(other);

		if (Objects.equal(actual, otherAsString)) {
			return;
		}
		
		printDiff(actual, otherAsString);
		failWithActual(simpleFact("expected to be equal"));
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

	public final void isEqualEventsTo(@NullableDecl Object other) {
		String otherAsString = toString(other);

		if (JsonComparator.isSameEvents(normalized, otherAsString)) {
			return;
		}

		printDiff(normalized, otherAsString);
		failWithActual(simpleFact("expected to be equal events"));
	}

	public static void printDiff(String result, String expected) {
		System.out.println("Expected (size " + expected.length() + "):\n" + expected);
		System.out.println("Actual (size " + result.length() + "):\n" + result);

		for(int k = 0; k < Math.min(expected.length(), result.length()); k++) {
			if(expected.charAt(k) != result.charAt(k)) {
				System.out.println("Diff at " + k + ": " + expected.charAt(k) + " vs + " + result.charAt(k));

				break;
			}
		}
	}

}
