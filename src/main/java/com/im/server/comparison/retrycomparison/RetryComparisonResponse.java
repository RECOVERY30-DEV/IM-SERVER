package com.im.server.comparison.retrycomparison;

import io.swagger.v3.oas.annotations.media.Schema;

public record RetryComparisonResponse(
    @Schema(description = "새로 생성된 비교 Job ID") Long comparisonId) {}
