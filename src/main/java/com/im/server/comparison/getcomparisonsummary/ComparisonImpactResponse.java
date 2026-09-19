package com.im.server.comparison.getcomparisonsummary;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record ComparisonImpactResponse(
    @Schema(description = "월 납입액 변화(원)") BigDecimal monthlyPaymentDelta,
    @Schema(description = "총이자 변화(원)") BigDecimal totalInterestDelta,
    @Schema(description = "확정비용 변화(원)") BigDecimal fixedFeeDelta,
    @Schema(description = "총비용 변화(원) = 총이자 변화 + 확정비용 변화") BigDecimal totalCostDelta) {}
