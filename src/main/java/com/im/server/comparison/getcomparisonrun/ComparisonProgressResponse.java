package com.im.server.comparison.getcomparisonrun;

import io.swagger.v3.oas.annotations.media.Schema;

public record ComparisonProgressResponse(
    @Schema(description = "진행률(%)", example = "100") int percent,
    @Schema(description = "완료된 항목 수") int completedSteps,
    @Schema(description = "전체 항목 수") int totalSteps) {}
