package com.github.skjolber.jsonfilter.jackson;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;

public class JacksonJsonLogFilterBuilderTest {

@Test
public void testBuilder() {
AbstractPathJsonFilter filter = (AbstractPathJsonFilter) JacksonJsonLogFilterBuilder.newBuilder()
.withMaxStringLength(127)
.withAnonymizePaths("/customer/email")
.withPrunePaths("/customer/account")
.withPruneMessage("pruneMessage")
.withAnonymizeMessage("anonymizeMessage")
.withTruncateMessage("truncateMessage")
.withMaxSize(32 * 1024)
.build();
assertNotNull(filter);

assertThat(filter.getMaxStringLength()).isEqualTo(127);
assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/customer/email"});
assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/customer/account"});
assertThat(filter.getMaxSize()).isEqualTo(32 * 1024);
}

@Test
public void testAnonymizeKeys() {
AbstractPathJsonFilter filter = (AbstractPathJsonFilter) JacksonJsonLogFilterBuilder.newBuilder()
.withAnonymizeKeys("password", "ssn")
.build();
assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"$..password", "$..ssn"});
}

@Test
public void testPruneKeys() {
AbstractPathJsonFilter filter = (AbstractPathJsonFilter) JacksonJsonLogFilterBuilder.newBuilder()
.withPruneKeys("rawPayload", "auditLog")
.build();
assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"$..rawPayload", "$..auditLog"});
}

@Test
public void testAnonymizeKeysCollection() {
List<String> keys = Arrays.asList("password", "ssn");
AbstractPathJsonFilter filter = (AbstractPathJsonFilter) JacksonJsonLogFilterBuilder.newBuilder()
.withAnonymizeKeys(keys)
.build();
assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"$..password", "$..ssn"});
}

@Test
public void testPruneKeysCollection() {
List<String> keys = Arrays.asList("rawPayload", "auditLog");
AbstractPathJsonFilter filter = (AbstractPathJsonFilter) JacksonJsonLogFilterBuilder.newBuilder()
.withPruneKeys(keys)
.build();
assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"$..rawPayload", "$..auditLog"});
}

@Test
public void testAnonymizePathsVarargs() {
AbstractPathJsonFilter filter = (AbstractPathJsonFilter) JacksonJsonLogFilterBuilder.newBuilder()
.withAnonymizePaths("$.customer.email", "$.customer.ssn")
.build();
assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"$.customer.email", "$.customer.ssn"});
}

@Test
public void testPrunePathsVarargs() {
AbstractPathJsonFilter filter = (AbstractPathJsonFilter) JacksonJsonLogFilterBuilder.newBuilder()
.withPrunePaths("$.internal.debug", "$.internal.trace")
.build();
assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"$.internal.debug", "$.internal.trace"});
}

@Test
public void testAnonymizePathsCollection() {
List<String> paths = Arrays.asList("$.customer.email", "$.customer.ssn");
AbstractPathJsonFilter filter = (AbstractPathJsonFilter) JacksonJsonLogFilterBuilder.newBuilder()
.withAnonymizePaths(paths)
.build();
assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"$.customer.email", "$.customer.ssn"});
}

@Test
public void testPrunePathsCollection() {
List<String> paths = Arrays.asList("$.internal.debug", "$.internal.trace");
AbstractPathJsonFilter filter = (AbstractPathJsonFilter) JacksonJsonLogFilterBuilder.newBuilder()
.withPrunePaths(paths)
.build();
assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"$.internal.debug", "$.internal.trace"});
}

// -------------------------------------------------------------------------
// Static one-liners: varargs
// -------------------------------------------------------------------------

@Test
public void testStaticAnonymizeKeysVarargs() {
assertNotNull(JacksonJsonLogFilterBuilder.anonymizeKeys("password", "ssn"));
}

@Test
public void testStaticAnonymizePathsVarargs() {
assertNotNull(JacksonJsonLogFilterBuilder.anonymizePaths("$.customer.email"));
}

@Test
public void testStaticPruneKeysVarargs() {
assertNotNull(JacksonJsonLogFilterBuilder.pruneKeys("appMeta"));
}

@Test
public void testStaticPrunePathsVarargs() {
assertNotNull(JacksonJsonLogFilterBuilder.prunePaths("$.context.appMeta"));
}

// -------------------------------------------------------------------------
// Static one-liners: Set<String>
// -------------------------------------------------------------------------

@Test
public void testStaticAnonymizeKeysSet() {
assertNotNull(JacksonJsonLogFilterBuilder.anonymizeKeys(Set.of("password", "ssn")));
}

@Test
public void testStaticAnonymizeKeysSetWithMaxStringLength() {
assertNotNull(JacksonJsonLogFilterBuilder.anonymizeKeys(Set.of("password", "ssn"), 256));
}

@Test
public void testStaticAnonymizeKeysSetWithMaxStringLengthAndMaxSize() {
assertNotNull(JacksonJsonLogFilterBuilder.anonymizeKeys(Set.of("password", "ssn"), 256, 128 * 1024));
}

@Test
public void testStaticAnonymizePathsSet() {
assertNotNull(JacksonJsonLogFilterBuilder.anonymizePaths(Set.of("$.customer.email")));
}

@Test
public void testStaticAnonymizePathsSetWithMaxStringLength() {
assertNotNull(JacksonJsonLogFilterBuilder.anonymizePaths(Set.of("$.customer.email"), 256));
}

@Test
public void testStaticAnonymizePathsSetWithMaxStringLengthAndMaxSize() {
assertNotNull(JacksonJsonLogFilterBuilder.anonymizePaths(Set.of("$.customer.email"), 256, 128 * 1024));
}

@Test
public void testStaticPruneKeysSet() {
assertNotNull(JacksonJsonLogFilterBuilder.pruneKeys(Set.of("appMeta")));
}

@Test
public void testStaticPruneKeysSetWithMaxStringLength() {
assertNotNull(JacksonJsonLogFilterBuilder.pruneKeys(Set.of("appMeta"), 256));
}

@Test
public void testStaticPruneKeysSetWithMaxStringLengthAndMaxSize() {
assertNotNull(JacksonJsonLogFilterBuilder.pruneKeys(Set.of("appMeta"), 256, 128 * 1024));
}

@Test
public void testStaticPrunePathsSet() {
assertNotNull(JacksonJsonLogFilterBuilder.prunePaths(Set.of("$.context.appMeta")));
}

@Test
public void testStaticPrunePathsSetWithMaxStringLength() {
assertNotNull(JacksonJsonLogFilterBuilder.prunePaths(Set.of("$.context.appMeta"), 256));
}

@Test
public void testStaticPrunePathsSetWithMaxStringLengthAndMaxSize() {
assertNotNull(JacksonJsonLogFilterBuilder.prunePaths(Set.of("$.context.appMeta"), 256, 128 * 1024));
}

/** Verify deprecated aliases still work for backwards compatibility. */
@Test
@SuppressWarnings("deprecation")
public void testDeprecatedAliases() {
JsonFilter filter = JacksonJsonLogFilterBuilder.createInstance()
.withMaxStringLength(127)
.withPruneStringValue("PRUNED")
.withAnonymizeStringValue("*")
.withTruncateStringValue("truncated\t")
.build();
assertNotNull(filter);
}
}
