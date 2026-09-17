package com.im.server.decision.reviewcomparisonitem;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record ReviewItemResponse(
    @Schema(description = "비교 항목 ID") Long itemId,
    @Schema(description = "확인 처리 시각") Instant reviewedAt) {}
