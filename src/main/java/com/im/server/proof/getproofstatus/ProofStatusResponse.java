package com.im.server.proof.getproofstatus;

import com.im.server.proof.api.AnchorStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

public record ProofStatusResponse(
    @Schema(description = "Proof 식별자 — GET /api/proof/{proofId}(\"기록 자세히 보기\")에 쓴다") String proofId,
    @Schema(description = "계약서 식별자", example = "contract_0001") String contractNumber,
    @Schema(description = "대출금액") long loanAmount,
    @Schema(description = "최종 적용금리(%)") BigDecimal finalRatePercent,
    @Schema(description = "약정(서명) 시각") Instant decidedAt,
    @Schema(description = "PENDING/SUBMITTED/CONFIRMED만 '검증됨' 배지는 CONFIRMED")
        AnchorStatus anchorStatus,
    @Schema(description = "기록 요약") ProofRecordSummaryResponse recordSummary) {}
