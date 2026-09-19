package com.im.server.comparison.getcomparisonitem;

import com.im.server.comparison.api.ItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * S04 상세 화면 응답. {@code reason}/{@code evidence}(FR10, comparison_item_evidence)는 아직 구현하지 않았다 —
 * docs/db-design.md 6.5 우선순위상 P1이라 이번 구현 범위에서 제외했다.
 */
public record ComparisonItemDetailResponse(
    @Schema(description = "비교 항목 ID") Long itemId,
    @Schema(description = "Field Code") String fieldCode,
    @Schema(description = "표시명") String label,
    @Schema(description = "판정 상태") ItemStatus itemStatus,
    @Schema(description = "V1 값") String v1ValueText,
    @Schema(description = "V2 값") String v2ValueText,
    @Schema(description = "변화량 표시") String deltaLabel,
    @Schema(description = "확인 필요 여부") boolean requiresReview,
    @Schema(description = "UNKNOWN 사유(해당 시)") String unknownReason) {}
