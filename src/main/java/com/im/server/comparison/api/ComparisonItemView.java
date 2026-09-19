package com.im.server.comparison.api;

public record ComparisonItemView(
    Long id,
    Long comparisonRunId,
    String fieldCode,
    String label,
    String unit,
    boolean contributesToOverall,
    ItemStatus itemStatus,
    String v1ValueText,
    String v2ValueText,
    String deltaLabel,
    boolean requiresReview,
    String unknownReason) {}
