package com.im.server.comparison.getcomparisonsummary;

import com.im.server.comparison.api.OverallStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ComparisonSummaryResponse(
    @Schema(description = "PRD 14.1 Overall Status") OverallStatus overallStatus,
    @Schema(description = "비용 영향") ComparisonImpactResponse impact,
    @Schema(description = "변경된 항목 수") int changedItemsCount,
    @Schema(description = "변경 없는 항목 수") int unchangedItemsCount,
    @Schema(description = "필수 확인 항목 수") int requiredReviewCount,
    @Schema(description = "대표 변경 항목") List<HeadlineItemResponse> headlineItems) {}
