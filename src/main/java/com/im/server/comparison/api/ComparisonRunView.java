package com.im.server.comparison.api;

public record ComparisonRunView(
    Long id,
    String applicationId,
    Long preSnapshotId,
    Long postSnapshotId,
    ComparisonRunStatus status,
    OverallStatus overallStatus,
    UncertainReason uncertainReason,
    int totalSteps,
    int completedSteps,
    int progressPercent,
    String promptVersion,
    String ruleVersion,
    String calculationVersion,
    String comparisonHash) {}
