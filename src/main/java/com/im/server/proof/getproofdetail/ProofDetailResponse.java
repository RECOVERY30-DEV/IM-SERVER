package com.im.server.proof.getproofdetail;

import com.im.server.proof.api.AnchorStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** implementation-spec.md 13.2 Canonical Proof Payload 필드와 1:1로 맞춘 응답. */
public record ProofDetailResponse(
    @Schema(description = "Proof 식별자") String proofId,
    String schemaVersion,
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
    String payloadHash,
    AnchorStatus anchorStatus,
    String ledgerReference,
    int confirmationCount,
    Instant submittedAt,
    Instant confirmedAt,
    short anchorRetryCount) {}
