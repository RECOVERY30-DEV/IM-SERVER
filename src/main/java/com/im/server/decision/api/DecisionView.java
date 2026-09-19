package com.im.server.decision.api;

import java.time.Instant;

public record DecisionView(
    Long id,
    Long comparisonRunId,
    DecisionType decisionType,
    boolean bypassedUncertainItems,
    boolean allItemsConfirmed,
    String signatureMethod,
    Instant signedAt,
    Long postSnapshotIdAtDecision,
    String contractDocumentHashAtDecision,
    Instant createdAt) {}
