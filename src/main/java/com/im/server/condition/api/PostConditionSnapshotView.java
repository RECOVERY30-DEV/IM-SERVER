package com.im.server.condition.api;

import java.time.Instant;

/** 다른 모듈에 노출하는 V2 Snapshot 뷰. */
public record PostConditionSnapshotView(
    Long id,
    String applicationId,
    Long preSnapshotId,
    String contractDocumentId,
    String contractDocumentHash,
    Instant decidedAt,
    String payloadHash,
    ConditionFields fields) {}
