package com.im.server.condition.getlatestprecondition;

import com.im.server.condition.api.ConditionFields;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record LatestPreConditionResponse(
    @Schema(description = "V1 Snapshot ID") Long preSnapshotId,
    @Schema(description = "VALID 또는 EXPIRED", example = "VALID") String status,
    @Schema(description = "조회 시각") Instant inquiredAt,
    @Schema(description = "유효기간") Instant expiresAt,
    @Schema(description = "사전조건 값") ConditionFields conditions) {}
