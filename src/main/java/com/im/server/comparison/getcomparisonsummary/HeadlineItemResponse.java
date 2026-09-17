package com.im.server.comparison.getcomparisonsummary;

import com.im.server.comparison.api.ItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record HeadlineItemResponse(
    @Schema(description = "비교 항목 ID") Long itemId,
    @Schema(description = "Field Code", example = "FINAL_RATE") String fieldCode,
    @Schema(description = "표시명", example = "대출금리") String label,
    @Schema(description = "판정 상태") ItemStatus itemStatus,
    @Schema(description = "변화량 표시", example = "+0.30%p") String deltaLabel) {}
