package com.im.server.proof.internal;

import java.time.Instant;

/**
 * implementation-spec.md 13.2 Canonical Proof Payload 그대로. 이 record의 canonical JSON을 해싱한 값이 {@code
 * payload_hash}이고, On-chain(목 원장)에는 이 payload 자체가 아니라 그 Hash만 올라간다.
 */
record CanonicalProofPayload(
    String schemaVersion,
    String proofId,
    String checkIdHash,
    String applicationIdHash,
    String v1SnapshotHash,
    String v2SnapshotHash,
    String comparisonResultHash,
    String decisionHash,
    String issuerId,
    String promptVersion,
    String ruleVersion,
    String calculationVersion,
    Instant createdAt,
    String supersedesRecordId) {}
