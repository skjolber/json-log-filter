package com.github.skjolber.jsonfilter.base;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class AbstractJsonLogFilterBuilderTest {

    private static class TestBuilder extends AbstractJsonLogFilterBuilder<TestBuilder> {
        @Override
        public com.github.skjolber.jsonfilter.JsonFilter build() {
            return new com.github.skjolber.jsonfilter.base.DefaultJsonFilter();
        }
    }

    @Test
    public void testWithMaxStringLength() {
        TestBuilder b = new TestBuilder();
        TestBuilder result = b.withMaxStringLength(100);
        assertThat(b.maxStringLength).isEqualTo(100);
        assertThat(result).isSameInstanceAs(b);
    }

    @Test
    public void testWithMaxPathMatches() {
        TestBuilder b = new TestBuilder();
        TestBuilder result = b.withMaxPathMatches(5);
        assertThat(b.maxPathMatches).isEqualTo(5);
        assertThat(result).isSameInstanceAs(b);
    }

    @Test
    public void testWithMaxSize() {
        TestBuilder b = new TestBuilder();
        TestBuilder result = b.withMaxSize(1024);
        assertThat(b.maxSize).isEqualTo(1024);
        assertThat(result).isSameInstanceAs(b);
    }

    @Test
    public void testWithRemoveWhitespace() {
        TestBuilder b = new TestBuilder();
        TestBuilder result = b.withRemoveWhitespace(true);
        assertThat(b.removeWhitespace).isTrue();
        assertThat(result).isSameInstanceAs(b);
        b.withRemoveWhitespace(false);
        assertThat(b.removeWhitespace).isFalse();
    }

    @Test
    public void testWithAnonymizePathsVarargs() {
        TestBuilder b = new TestBuilder();
        b.withAnonymizePaths("$.a", "$.b");
        assertThat(b.anonymizeFilters).containsExactly("$.a", "$.b");
    }

    @Test
    public void testWithAnonymizePathsCollection() {
        TestBuilder b = new TestBuilder();
        b.withAnonymizePaths(Arrays.asList("$.c", "$.d"));
        assertThat(b.anonymizeFilters).containsExactly("$.c", "$.d");
    }

    @Test
    public void testWithAnonymizeKeysVarargs() {
        TestBuilder b = new TestBuilder();
        b.withAnonymizeKeys("password", "token");
        assertThat(b.anonymizeFilters).containsExactly("$..password", "$..token");
    }

    @Test
    public void testWithAnonymizeKeysCollection() {
        TestBuilder b = new TestBuilder();
        b.withAnonymizeKeys(Arrays.asList("password", "token"));
        assertThat(b.anonymizeFilters).containsExactly("$..password", "$..token");
    }

    @Test
    public void testWithPrunePathsVarargs() {
        TestBuilder b = new TestBuilder();
        b.withPrunePaths("$.x", "$.y");
        assertThat(b.pruneFilters).containsExactly("$.x", "$.y");
    }

    @Test
    public void testWithPrunePathsCollection() {
        TestBuilder b = new TestBuilder();
        b.withPrunePaths(Arrays.asList("$.z"));
        assertThat(b.pruneFilters).containsExactly("$.z");
    }

    @Test
    public void testWithPruneKeysVarargs() {
        TestBuilder b = new TestBuilder();
        b.withPruneKeys("rawPayload", "auditLog");
        assertThat(b.pruneFilters).containsExactly("$..rawPayload", "$..auditLog");
    }

    @Test
    public void testWithPruneKeysCollection() {
        TestBuilder b = new TestBuilder();
        b.withPruneKeys(Arrays.asList("rawPayload"));
        assertThat(b.pruneFilters).containsExactly("$..rawPayload");
    }

    @Test
    public void testWithPruneMessage() {
        TestBuilder b = new TestBuilder();
        b.withPruneMessage("[removed]");
        assertThat(b.pruneJsonValue).isNotNull();
        assertThat(b.pruneJsonValue).startsWith("\"");
        assertThat(b.pruneJsonValue).endsWith("\"");
        assertThat(b.pruneJsonValue).contains("[removed]");
    }

    @Test
    public void testWithAnonymizeMessage() {
        TestBuilder b = new TestBuilder();
        b.withAnonymizeMessage("[redacted]");
        assertThat(b.anonymizeJsonValue).isNotNull();
        assertThat(b.anonymizeJsonValue).startsWith("\"");
        assertThat(b.anonymizeJsonValue).endsWith("\"");
        assertThat(b.anonymizeJsonValue).contains("[redacted]");
    }

    @Test
    public void testWithTruncateMessage() {
        TestBuilder b = new TestBuilder();
        b.withTruncateMessage("...");
        assertThat(b.truncateStringValue).isNotNull();
        assertThat(b.truncateStringValue).isEqualTo("...");
    }

    @Test
    public void testWithPruneMessageEscaping() {
        TestBuilder b = new TestBuilder();
        b.withPruneMessage("tab\there");
        assertThat(b.pruneJsonValue).contains("\\t");
    }

    @Test
    public void testWithAnonymizeMessageEscaping() {
        TestBuilder b = new TestBuilder();
        b.withAnonymizeMessage("line\nbreak");
        assertThat(b.anonymizeJsonValue).contains("\\n");
    }

    @Test
    public void testWithTruncateMessageEscaping() {
        TestBuilder b = new TestBuilder();
        b.withTruncateMessage("cr\rhere");
        assertThat(b.truncateStringValue).contains("\\r");
    }

    @Test
    public void testWithPruneRawJsonValue() {
        TestBuilder b = new TestBuilder();
        b.withPruneRawJsonValue("\"PRUNED\"");
        assertThat(b.pruneJsonValue).isEqualTo("\"PRUNED\"");
    }

    @Test
    public void testWithAnonymizeRawJsonValue() {
        TestBuilder b = new TestBuilder();
        b.withAnonymizeRawJsonValue("\"*\"");
        assertThat(b.anonymizeJsonValue).isEqualTo("\"*\"");
    }

    @Test
    public void testWithTruncateRawJsonStringValue() {
        TestBuilder b = new TestBuilder();
        b.withTruncateRawJsonStringValue("... + ");
        assertThat(b.truncateStringValue).isEqualTo("... + ");
    }

    @Test
    public void testDeprecatedWithAnonymizeVarargs() {
        TestBuilder b = new TestBuilder();
        b.withAnonymize("$.a", "$.b");
        assertThat(b.anonymizeFilters).containsExactly("$.a", "$.b");
    }

    @Test
    public void testDeprecatedWithAnonymizeCollection() {
        TestBuilder b = new TestBuilder();
        b.withAnonymize(Arrays.asList("$.a"));
        assertThat(b.anonymizeFilters).containsExactly("$.a");
    }

    @Test
    public void testDeprecatedWithPruneVarargs() {
        TestBuilder b = new TestBuilder();
        b.withPrune("$.x", "$.y");
        assertThat(b.pruneFilters).containsExactly("$.x", "$.y");
    }

    @Test
    public void testDeprecatedWithPruneCollection() {
        TestBuilder b = new TestBuilder();
        b.withPrune(Arrays.asList("$.x"));
        assertThat(b.pruneFilters).containsExactly("$.x");
    }

    @Test
    public void testDeprecatedWithPruneStringValue() {
        TestBuilder b = new TestBuilder();
        b.withPruneStringValue("[removed]");
        assertThat(b.pruneJsonValue).isNotNull();
        assertThat(b.pruneJsonValue).startsWith("\"");
    }

    @Test
    public void testDeprecatedWithAnonymizeStringValue() {
        TestBuilder b = new TestBuilder();
        b.withAnonymizeStringValue("[redacted]");
        assertThat(b.anonymizeJsonValue).isNotNull();
        assertThat(b.anonymizeJsonValue).startsWith("\"");
    }

    @Test
    public void testDeprecatedWithTruncateStringValue() {
        TestBuilder b = new TestBuilder();
        b.withTruncateStringValue("...");
        assertThat(b.truncateStringValue).isNotNull();
    }

    @Test
    public void testBuild() {
        TestBuilder b = new TestBuilder();
        assertNotNull(b.build());
    }

    @Test
    public void testFluentChaining() {
        TestBuilder b = new TestBuilder();
        assertThat(b.withMaxStringLength(100)).isSameInstanceAs(b);
        assertThat(b.withMaxPathMatches(5)).isSameInstanceAs(b);
        assertThat(b.withMaxSize(1024)).isSameInstanceAs(b);
        assertThat(b.withRemoveWhitespace(true)).isSameInstanceAs(b);
        assertThat(b.withAnonymizePaths("$.a")).isSameInstanceAs(b);
        assertThat(b.withPrunePaths("$.b")).isSameInstanceAs(b);
        assertThat(b.withAnonymizeKeys("k1")).isSameInstanceAs(b);
        assertThat(b.withPruneKeys("k2")).isSameInstanceAs(b);
        assertThat(b.withPruneRawJsonValue("\"p\"")).isSameInstanceAs(b);
        assertThat(b.withAnonymizeRawJsonValue("\"a\"")).isSameInstanceAs(b);
        assertThat(b.withTruncateRawJsonStringValue("t")).isSameInstanceAs(b);
    }
}
