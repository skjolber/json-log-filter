package com.github.skjolber.jsonfilter.core;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;

public class DefaultJsonLogFilterBuilderTest {

@Test
public void testBuilder() {
AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
.withMaxStringLength(127)
.withAnonymizePaths("/customer/email")
.withPrunePaths("/customer/account")
.withPruneMessage("pruneMessage")
.withAnonymizeMessage("pruneMessage")
.withTruncateMessage("truncateMessage")
.withMaxPathMatches(10)
.withMaxSize(32 * 1024)
.build();
assertNotNull(filter);

assertThat(filter.getMaxStringLength()).isEqualTo(127);
assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/customer/email"});
assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/customer/account"});
assertThat(filter.getMaxPathMatches()).isEqualTo(10);
assertThat(filter.getMaxSize()).isEqualTo(32 * 1024);
}

@Test
public void testAnonymizeKeys() {
AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
.withAnonymizeKeys("password", "ssn")
.build();
assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"$..password", "$..ssn"});
}

@Test
public void testPruneKeys() {
AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
.withPruneKeys("rawPayload", "auditLog")
.build();
assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"$..rawPayload", "$..auditLog"});
}

@Test
public void testAnonymizeKeysCollection() {
List<String> keys = Arrays.asList("password", "ssn");
AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
.withAnonymizeKeys(keys)
.build();
assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"$..password", "$..ssn"});
}

@Test
public void testPruneKeysCollection() {
List<String> keys = Arrays.asList("rawPayload", "auditLog");
AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
.withPruneKeys(keys)
.build();
assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"$..rawPayload", "$..auditLog"});
}

@Test
public void testAnonymizePathsVarargs() {
AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
.withAnonymizePaths("$.customer.email", "$.customer.ssn")
.build();
assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"$.customer.email", "$.customer.ssn"});
}

@Test
public void testPrunePathsVarargs() {
AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
.withPrunePaths("$.internal.debug", "$.internal.trace")
.build();
assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"$.internal.debug", "$.internal.trace"});
}

@Test
public void testAnonymizePathsCollection() {
List<String> paths = Arrays.asList("$.customer.email", "$.customer.ssn");
AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
.withAnonymizePaths(paths)
.build();
assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"$.customer.email", "$.customer.ssn"});
}

@Test
public void testPrunePathsCollection() {
List<String> paths = Arrays.asList("$.internal.debug", "$.internal.trace");
AbstractPathJsonFilter filter = (AbstractPathJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
.withPrunePaths(paths)
.build();
assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"$.internal.debug", "$.internal.trace"});
}

// -------------------------------------------------------------------------
// Static one-liners: varargs
// -------------------------------------------------------------------------

@Test
public void testStaticAnonymizeKeysVarargs() {
assertNotNull(DefaultJsonLogFilterBuilder.anonymizeKeys("password", "ssn"));
}

@Test
public void testStaticAnonymizePathsVarargs() {
assertNotNull(DefaultJsonLogFilterBuilder.anonymizePaths("$.customer.email"));
}

@Test
public void testStaticPruneKeysVarargs() {
assertNotNull(DefaultJsonLogFilterBuilder.pruneKeys("appMeta"));
}

@Test
public void testStaticPrunePathsVarargs() {
assertNotNull(DefaultJsonLogFilterBuilder.prunePaths("$.context.appMeta"));
}

// -------------------------------------------------------------------------
// Static one-liners: Set<String>
// -------------------------------------------------------------------------

@Test
public void testStaticAnonymizeKeysSet() {
assertNotNull(DefaultJsonLogFilterBuilder.anonymizeKeys(Set.of("password", "ssn")));
}

@Test
public void testStaticAnonymizeKeysSetWithMaxStringLength() {
assertNotNull(DefaultJsonLogFilterBuilder.anonymizeKeys(Set.of("password", "ssn"), 256));
}

@Test
public void testStaticAnonymizeKeysSetWithMaxStringLengthAndMaxSize() {
assertNotNull(DefaultJsonLogFilterBuilder.anonymizeKeys(Set.of("password", "ssn"), 256, 128 * 1024));
}

@Test
public void testStaticAnonymizePathsSet() {
assertNotNull(DefaultJsonLogFilterBuilder.anonymizePaths(Set.of("$.customer.email")));
}

@Test
public void testStaticAnonymizePathsSetWithMaxStringLength() {
assertNotNull(DefaultJsonLogFilterBuilder.anonymizePaths(Set.of("$.customer.email"), 256));
}

@Test
public void testStaticAnonymizePathsSetWithMaxStringLengthAndMaxSize() {
assertNotNull(DefaultJsonLogFilterBuilder.anonymizePaths(Set.of("$.customer.email"), 256, 128 * 1024));
}

@Test
public void testStaticPruneKeysSet() {
assertNotNull(DefaultJsonLogFilterBuilder.pruneKeys(Set.of("appMeta")));
}

@Test
public void testStaticPruneKeysSetWithMaxStringLength() {
assertNotNull(DefaultJsonLogFilterBuilder.pruneKeys(Set.of("appMeta"), 256));
}

@Test
public void testStaticPruneKeysSetWithMaxStringLengthAndMaxSize() {
assertNotNull(DefaultJsonLogFilterBuilder.pruneKeys(Set.of("appMeta"), 256, 128 * 1024));
}

@Test
public void testStaticPrunePathsSet() {
assertNotNull(DefaultJsonLogFilterBuilder.prunePaths(Set.of("$.context.appMeta")));
}

@Test
public void testStaticPrunePathsSetWithMaxStringLength() {
assertNotNull(DefaultJsonLogFilterBuilder.prunePaths(Set.of("$.context.appMeta"), 256));
}

@Test
public void testStaticPrunePathsSetWithMaxStringLengthAndMaxSize() {
assertNotNull(DefaultJsonLogFilterBuilder.prunePaths(Set.of("$.context.appMeta"), 256, 128 * 1024));
}

// -------------------------------------------------------------------------
// Truncate message and deprecated aliases
// -------------------------------------------------------------------------

@Test
public void testTruncateMessage() {
MaxStringLengthJsonFilter filter = (MaxStringLengthJsonFilter) DefaultJsonLogFilterBuilder.newBuilder()
.withMaxStringLength(127)
.withTruncateMessage("truncated\t")
.build();
assertNotNull(filter);
assertThat(new String(filter.getTruncateStringValue())).isEqualTo("truncated\\t");
assertThat(new String(filter.getPruneJsonValue())).isEqualTo("\"[removed]\"");
assertThat(new String(filter.getAnonymizeJsonValue())).isEqualTo("\"*\"");
}

/** Verify deprecated aliases still work for backwards compatibility. */
@Test
@SuppressWarnings("deprecation")
public void testDeprecatedAliases() {
MaxStringLengthJsonFilter filter = (MaxStringLengthJsonFilter) DefaultJsonLogFilterBuilder.createInstance()
.withMaxStringLength(127)
.withPruneStringValue("[removed]")
.withAnonymizeStringValue("*")
.withTruncateStringValue("truncated\t")
.build();
assertNotNull(filter);
assertThat(new String(filter.getTruncateStringValue())).isEqualTo("truncated\\t");
}
}
