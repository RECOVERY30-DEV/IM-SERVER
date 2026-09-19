package com.im.server.condition.api;

import java.time.Instant;

/** 다른 모듈에 노출하는 V1 Snapshot 뷰. */
public record PreConditionSnapshotView(
    Long id,
    String applicationId,
    String customerId,
    Instant inquiredAt,
    Instant expiresAt,
    String payloadHash,
    ConditionFields fields) {}
