package com.github.skjolber.jsonfilter.web;

import org.teavm.jso.JSExport;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.DefaultJsonLogFilterBuilder;

/**
 * TeaVM entry point — exported to JavaScript via {@link JSExport}.
 *
 * <p>Uses {@link DefaultJsonLogFilterBuilder} from {@code impl/core} to build
 * real filter instances for each call (factory pattern), then calls
 * {@link JsonFilter#process(String)} — the same API used in production Java code.
 *
 * <p>After {@code app.js} is loaded the HTML page bridges the exported symbol to
 * {@code window.JsonFilterApp}:
 * <pre>{@code
 * // TeaVM NONE module exposes @JSExport methods via the "teavm" global
 * window.JsonFilterApp = {
 *   applyFilter: function() { return teavm.applyFilter.apply(teavm, arguments); }
 * };
 * }</pre>
 */
public class JsonFilterApp {

    /** TeaVM requires a {@code main} entry point; nothing needs to run at startup. */
    public static void main(String[] args) {
        // intentionally empty — all work is done through @JSExport methods
    }

    /**
     * Build a filter from the supplied configuration and apply it to the input JSON.
     *
     * <p>Delegates to {@link DefaultJsonLogFilterBuilder} — the same builder used
     * in production Java applications.
     *
     * @param input            JSON string to filter
     * @param anonymizeKeys    newline/comma-separated bare field names to anonymize at any depth
     * @param pruneKeys        newline/comma-separated bare field names to prune at any depth
     * @param anonymizePaths   newline/comma-separated JSONPath expressions whose values are anonymized
     * @param prunePaths       newline/comma-separated JSONPath expressions whose subtrees are removed
     * @param maxStringLength  maximum string value length; values beyond this are truncated (-1 = unlimited)
     * @param maxSize          maximum output size in characters (-1 = unlimited)
     * @param removeWhitespace when true, all insignificant whitespace is stripped from the output
     * @param anonymizeMessage replacement text shown instead of anonymized values (empty = default "*")
     * @param pruneMessage     replacement shown instead of pruned subtrees (empty = default "[removed]")
     * @param truncateMessage  suffix appended to truncated strings (empty = default "... + ")
     * @param maxPathMatches   stop path/key matching after this many hits (-1 = unlimited)
     * @return filtered JSON string, or an error message prefixed with "Error:" on failure
     */
    @JSExport
    public static String applyFilter(
            String input,
            String anonymizeKeys,
            String pruneKeys,
            String anonymizePaths,
            String prunePaths,
            int maxStringLength,
            int maxSize,
            boolean removeWhitespace,
            String anonymizeMessage,
            String pruneMessage,
            String truncateMessage,
            int maxPathMatches) {

        try {
            JsonFilter filter = buildFilter(anonymizeKeys, pruneKeys, anonymizePaths, prunePaths,
                    maxStringLength, maxSize, removeWhitespace, anonymizeMessage, pruneMessage,
                    truncateMessage, maxPathMatches);
            String result = filter.process(input);
            return result != null ? result : "Error: filter returned null — input may not be valid JSON.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Return the simple class name of the filter that would be built for the given configuration.
     * Used by the UI to display which implementation is active.
     */
    @JSExport
    public static String getFilterClass(
            String anonymizeKeys,
            String pruneKeys,
            String anonymizePaths,
            String prunePaths,
            int maxStringLength,
            int maxSize,
            boolean removeWhitespace,
            String anonymizeMessage,
            String pruneMessage,
            String truncateMessage,
            int maxPathMatches) {

        try {
            return buildFilter(anonymizeKeys, pruneKeys, anonymizePaths, prunePaths,
                    maxStringLength, maxSize, removeWhitespace, anonymizeMessage, pruneMessage,
                    truncateMessage, maxPathMatches).getClass().getSimpleName();
        } catch (Exception e) {
            return "";
        }
    }

    private static JsonFilter buildFilter(
            String anonymizeKeys, String pruneKeys, String anonymizePaths, String prunePaths,
            int maxStringLength, int maxSize, boolean removeWhitespace,
            String anonymizeMessage, String pruneMessage, String truncateMessage, int maxPathMatches) {

        DefaultJsonLogFilterBuilder builder = DefaultJsonLogFilterBuilder.newBuilder();

        String[] anonKeys  = splitNonEmpty(anonymizeKeys);
        String[] pKeys     = splitNonEmpty(pruneKeys);
        String[] anonPaths = splitNonEmpty(anonymizePaths);
        String[] pPaths    = splitNonEmpty(prunePaths);

        if (anonKeys.length  > 0) builder.withAnonymizeKeys(anonKeys);
        if (pKeys.length     > 0) builder.withPruneKeys(pKeys);
        if (anonPaths.length > 0) builder.withAnonymizePaths(anonPaths);
        if (pPaths.length    > 0) builder.withPrunePaths(pPaths);

        if (maxStringLength > 0) builder.withMaxStringLength(maxStringLength);
        if (maxSize > 0)         builder.withMaxSize(maxSize);
        if (removeWhitespace)    builder.withRemoveWhitespace(true);
        if (maxPathMatches > 0)  builder.withMaxPathMatches(maxPathMatches);

        if (anonymizeMessage != null && !anonymizeMessage.isEmpty())
            builder.withAnonymizeMessage(anonymizeMessage);
        if (pruneMessage != null && !pruneMessage.isEmpty())
            builder.withPruneMessage(pruneMessage);
        if (truncateMessage != null && !truncateMessage.isEmpty())
            builder.withTruncateStringValue(truncateMessage);

        return builder.build();
    }

    /** Split a newline/comma-separated string, trimming tokens and dropping blanks. */
    private static String[] splitNonEmpty(String raw) {
        if (raw == null || raw.trim().isEmpty()) return new String[0];
        String[] parts = raw.split("[,\n\r]");
        int count = 0;
        for (String p : parts) { if (!p.trim().isEmpty()) count++; }
        String[] result = new String[count];
        int i = 0;
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) result[i++] = t;
        }
        return result;
    }
}
