package com.im.server.decision.getreviewgate;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ReviewGateResponse(
    @Schema(description = "필수 확인 항목") List<RequiredReviewItemResponse> requiredItems,
    @Schema(description = "전체 확인 완료 여부 — true여야 PROCEED 가능") boolean allReviewed) {}
