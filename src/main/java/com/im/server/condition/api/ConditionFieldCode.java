package com.im.server.condition.api;

/**
 * PRD 10장 비교 대상 Field Code (MVP 범위만).
 *
 * <p>{@code baseRatePercent}/{@code spreadRatePercent}는 {@link ConditionFields}에는 있지만 여기엔 없다 — PRD
 * 10장이 BASE_RATE를 "최종금리의 설명 Driver"로만 규정할 뿐 독립적인 판정 기준(상승/하락 → 유불리)을 주지 않기 때문에, 실제 비교·판정은 그 값들이 반영된
 * FINAL_RATE 하나로만 수행한다.
 */
public enum ConditionFieldCode {
  LOAN_AMOUNT("대출금액", "WON"),
  PREFERENTIAL_RATE("우대금리", "PERCENT"),
  FINAL_RATE("최종 적용금리", "PERCENT"),
  PREFERENTIAL_CONDITIONS("우대조건", "ENUM"),
  TERM_MONTHS("대출기간", "MONTHS"),
  REPAYMENT_METHOD("상환방식", "ENUM"),
  FIXED_UPFRONT_FEES("인지세·확정비용", "WON"),
  DELINQUENCY_RATE("연체이자율", "PERCENT");

  private final String label;
  private final String unit;

  ConditionFieldCode(String label, String unit) {
    this.label = label;
    this.unit = unit;
  }

  public String getLabel() {
    return label;
  }

  public String getUnit() {
    return unit;
  }
}
