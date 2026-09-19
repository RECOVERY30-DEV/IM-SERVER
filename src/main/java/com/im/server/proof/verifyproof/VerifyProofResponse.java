package com.im.server.proof.verifyproof;

import com.im.server.proof.domain.VerifyResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record VerifyProofResponse(
    @Schema(description = "검증 성공 여부") boolean verified,
    @Schema(description = "VERIFIED / NOT_ANCHORED / HASH_MISMATCH") VerifyResult reason,
    @Schema(description = "재계산한 Hash") String recomputedHash,
    @Schema(description = "원장 참조") String ledgerReference,
    @Schema(description = "원장에 기록된 시각") Instant recordedAt) {}
