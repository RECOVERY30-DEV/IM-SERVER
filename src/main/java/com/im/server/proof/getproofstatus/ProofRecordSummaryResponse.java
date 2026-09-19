package com.im.server.proof.getproofstatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProofRecordSummaryResponse(
    @Schema(description = "변경된 항목 수") int changedItemsCount,
    @Schema(description = "필수 확인 전체 완료 여부") boolean allReviewed,
    @Schema(description = "원문 근거 연결 여부 (AI 근거 추출 미구현이라 항상 false)") boolean evidenceLinked) {}
