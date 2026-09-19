package com.im.server.proof.api;

import java.time.Instant;

public record LedgerRecordView(
    String payloadHash,
    String schemaVersion,
    String issuer,
    Instant recordedAt,
    String supersedesRecordId) {}
