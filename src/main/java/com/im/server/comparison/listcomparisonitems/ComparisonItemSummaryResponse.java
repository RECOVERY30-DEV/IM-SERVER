package com.im.server.comparison.listcomparisonitems;

import com.im.server.comparison.api.ItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record ComparisonItemSummaryResponse(
    @Schema(description = "비교 항목 ID") Long itemId,
    @Schema(description = "Field Code") String fieldCode,
    @Schema(description = "표시명") String label,
    @Schema(description = "판정 상태") ItemStatus itemStatus,
    @Schema(description = "V1 값") String v1ValueText,
    @Schema(description = "V2 값") String v2ValueText,
    @Schema(description = "변화량 표시") String deltaLabel,
    @Schema(description = "확인 필요 여부") boolean requiresReview) {}
