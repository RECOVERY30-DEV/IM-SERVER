package com.im.server.decision.getreviewgate;

import io.swagger.v3.oas.annotations.media.Schema;

public record RequiredReviewItemResponse(
    @Schema(description = "비교 항목 ID") Long itemId,
    @Schema(description = "표시명") String label,
    @Schema(description = "확인 여부") boolean reviewed) {}
