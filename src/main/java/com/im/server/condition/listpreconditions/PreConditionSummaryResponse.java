package com.im.server.condition.listpreconditions;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

public record PreConditionSummaryResponse(
    @Schema(description = "V1 Snapshot ID") Long preSnapshotId,
    @Schema(description = "조회 시각") Instant inquiredAt,
    @Schema(description = "유효기간") Instant expiresAt,
    @Schema(description = "대출한도") long loanAmount,
    @Schema(description = "최종 적용금리(%)") BigDecimal finalRatePercent) {}
