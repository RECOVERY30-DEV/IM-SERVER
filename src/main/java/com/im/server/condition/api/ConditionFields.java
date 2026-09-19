package com.im.server.condition.api;

import java.math.BigDecimal;
import java.util.List;

/**
 * PRD 10장 Field Code를 타입 있는 값으로 담은 V1/V2 공통 스키마. {@code condition_pre_snapshots}·{@code
 * condition_post_snapshots}의 {@code snapshot_payload} JSON 컬럼이 이 record 구조를 그대로 직렬화한 것이다.
 *
 * <p>MVP 범위: NET_DISBURSEMENT_AMOUNT, RATE_TYPE은 아직 다루지 않는다(도입 시 필드 추가).
 */
public record ConditionFields(
    long loanAmount,
    BigDecimal baseRatePercent,
    BigDecimal spreadRatePercent,
    BigDecimal preferentialRatePercent,
    BigDecimal finalRatePercent,
    int termMonths,
    String repaymentMethod,
    List<PreferentialCondition> preferentialConditions,
    long stampTax,
    long fixedUpfrontFees,
    BigDecimal delinquencyRatePercent) {}
