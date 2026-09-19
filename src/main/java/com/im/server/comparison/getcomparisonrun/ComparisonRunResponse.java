package com.im.server.comparison.getcomparisonrun;

import com.im.server.comparison.api.ComparisonRunStatus;
import com.im.server.comparison.api.OverallStatus;
import com.im.server.comparison.api.UncertainReason;
import io.swagger.v3.oas.annotations.media.Schema;

public record ComparisonRunResponse(
    @Schema(description = "비교 Job ID") Long comparisonId,
    @Schema(description = "대출 신청 ID") String applicationId,
    @Schema(description = "Job 상태") ComparisonRunStatus status,
    @Schema(description = "PRD 14.1 Overall Status") OverallStatus overallStatus,
    @Schema(description = "UNCERTAIN 사유") UncertainReason uncertainReason,
    @Schema(description = "진행상태") ComparisonProgressResponse progress) {}
