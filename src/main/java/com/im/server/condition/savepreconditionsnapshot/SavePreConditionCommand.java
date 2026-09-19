package com.im.server.condition.savepreconditionsnapshot;

import com.im.server.condition.api.ConditionFields;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record SavePreConditionCommand(
    @Schema(description = "외부 고객 ID", example = "cust_0001") String customerId,
    @Schema(description = "상품코드", example = "PL-CREDIT-01") String productCode,
    @Schema(description = "사전조회 시점 상품/우대조건 정책 Version", example = "2026-09-01")
        String productVersion,
    @Schema(description = "사전조회 시각") Instant inquiredAt,
    @Schema(description = "V1 유효기간") Instant expiresAt,
    @Schema(description = "사전조건 값") ConditionFields conditions) {}
