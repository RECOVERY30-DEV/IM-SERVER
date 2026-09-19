package com.im.server.comparison.internal;

import com.im.server.comparison.api.ItemStatus;
import com.im.server.comparison.api.OverallStatus;
import com.im.server.comparison.domain.ComparisonItem;
import com.im.server.condition.api.ConditionFieldCode;
import com.im.server.condition.api.ConditionFields;
import com.im.server.condition.api.PreferentialCondition;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * FR04~FR07 중 구조화 입력(양쪽 Snapshot 모두 API로 확보한 구조화 값)만 다루는 비교 Rule Engine이다. 비정형 문서에서 값을 뽑아내는 AI
 * 추출(FR04의 DOCUMENT_AI 경로)은 아직 붙어있지 않다 — {@code docs/db-design.md} 6장에 명시된 대로, 문서 파싱이 붙기 전까지는 이
 * 경로(STRUCTURED_API)만 채운다.
 */
@Component
public class ComparisonEngine {

  private static final String SOURCE_STRUCTURED_API = "STRUCTURED_API";

  public List<ComparisonItem> compare(ConditionFields v1, ConditionFields v2) {
    return List.of(
        compareLoanAmount(v1, v2),
        // 우대금리는 "확대 유리, 축소 불리" — 다른 금리류(낮을수록 유리)와 방향이 반대다(PRD 10장).
        compareRate(
            ConditionFieldCode.PREFERENTIAL_RATE,
            v1.preferentialRatePercent(),
            v2.preferentialRatePercent(),
            true),
        compareRate(
            ConditionFieldCode.FINAL_RATE, v1.finalRatePercent(), v2.finalRatePercent(), false),
        comparePreferentialConditions(v1, v2),
        compareTermMonths(v1, v2),
        compareRepaymentMethod(v1, v2),
        compareFixedUpfrontFees(v1, v2),
        compareRate(
            ConditionFieldCode.DELINQUENCY_RATE,
            v1.delinquencyRatePercent(),
            v2.delinquencyRatePercent(),
            false));
  }

  public OverallStatus computeOverallStatus(List<ComparisonItem> items) {
    List<ComparisonItem> required =
        items.stream().filter(ComparisonItem::isContributesToOverall).toList();

    if (required.stream().anyMatch(i -> i.getItemStatus() == ItemStatus.UNKNOWN)) {
      return OverallStatus.UNCERTAIN;
    }
    if (required.stream()
        .anyMatch(
            i ->
                i.getItemStatus() == ItemStatus.WORSE
                    || i.getItemStatus() == ItemStatus.STRUCTURAL_CHANGE)) {
      return OverallStatus.CHECK_REQUIRED;
    }
    boolean anyBetter = required.stream().anyMatch(i -> i.getItemStatus() == ItemStatus.BETTER);
    boolean allSameOrBetter =
        required.stream()
            .allMatch(
                i ->
                    i.getItemStatus() == ItemStatus.SAME || i.getItemStatus() == ItemStatus.BETTER);
    if (anyBetter && allSameOrBetter) {
      return OverallStatus.BURDEN_DECREASE;
    }
    if (required.stream().allMatch(i -> i.getItemStatus() == ItemStatus.SAME)) {
      return OverallStatus.NO_CHANGE;
    }
    return OverallStatus.UNCERTAIN;
  }

  private ComparisonItem compareLoanAmount(ConditionFields v1, ConditionFields v2) {
    ItemStatus status =
        v1.loanAmount() == v2.loanAmount() ? ItemStatus.SAME : ItemStatus.STRUCTURAL_CHANGE;
    return new ComparisonItem(
        null,
        ConditionFieldCode.LOAN_AMOUNT.name(),
        true,
        status,
        formatWon(v1.loanAmount()),
        formatWon(v2.loanAmount()),
        BigDecimal.valueOf(v1.loanAmount()),
        BigDecimal.valueOf(v2.loanAmount()),
        "WON",
        BigDecimal.valueOf(v2.loanAmount() - v1.loanAmount()),
        SOURCE_STRUCTURED_API,
        null);
  }

  /**
   * 금리류 비교. {@code higherIsBetter=false}면 상승=WORSE/하락=BETTER(최종금리·연체이자율), {@code true}면 반대(우대금리 —
   * 확대가 유리하다).
   */
  private ComparisonItem compareRate(
      ConditionFieldCode fieldCode, BigDecimal v1Rate, BigDecimal v2Rate, boolean higherIsBetter) {
    int cmp = v2Rate.compareTo(v1Rate);
    ItemStatus status;
    if (cmp == 0) {
      status = ItemStatus.SAME;
    } else {
      boolean increased = cmp > 0;
      status = (increased == higherIsBetter) ? ItemStatus.BETTER : ItemStatus.WORSE;
    }
    return new ComparisonItem(
        null,
        fieldCode.name(),
        true,
        status,
        formatRate(v1Rate),
        formatRate(v2Rate),
        v1Rate,
        v2Rate,
        "PERCENT",
        v2Rate.subtract(v1Rate),
        SOURCE_STRUCTURED_API,
        null);
  }

  private ComparisonItem comparePreferentialConditions(ConditionFields v1, ConditionFields v2) {
    Set<String> v1Codes =
        v1.preferentialConditions().stream()
            .map(PreferentialCondition::code)
            .collect(Collectors.toSet());
    Set<String> v2Codes =
        v2.preferentialConditions().stream()
            .map(PreferentialCondition::code)
            .collect(Collectors.toSet());

    ItemStatus status;
    if (v1Codes.equals(v2Codes)) {
      status = ItemStatus.SAME;
    } else if (v2Codes.containsAll(v1Codes)) {
      status = ItemStatus.BETTER;
    } else {
      // 하나라도 빠지면(혼재 포함) 안전하게 WORSE로 분류한다 — False Safe 금지 원칙(PRD 5.1, 20장).
      status = ItemStatus.WORSE;
    }
    return new ComparisonItem(
        null,
        ConditionFieldCode.PREFERENTIAL_CONDITIONS.name(),
        true,
        status,
        joinLabels(v1),
        joinLabels(v2),
        null,
        null,
        "ENUM",
        null,
        SOURCE_STRUCTURED_API,
        null);
  }

  private ComparisonItem compareTermMonths(ConditionFields v1, ConditionFields v2) {
    ItemStatus status =
        v1.termMonths() == v2.termMonths() ? ItemStatus.SAME : ItemStatus.STRUCTURAL_CHANGE;
    return new ComparisonItem(
        null,
        ConditionFieldCode.TERM_MONTHS.name(),
        true,
        status,
        v1.termMonths() + "개월",
        v2.termMonths() + "개월",
        BigDecimal.valueOf(v1.termMonths()),
        BigDecimal.valueOf(v2.termMonths()),
        "MONTHS",
        BigDecimal.valueOf(v2.termMonths() - v1.termMonths()),
        SOURCE_STRUCTURED_API,
        null);
  }

  private ComparisonItem compareRepaymentMethod(ConditionFields v1, ConditionFields v2) {
    ItemStatus status =
        v1.repaymentMethod().equals(v2.repaymentMethod())
            ? ItemStatus.SAME
            : ItemStatus.STRUCTURAL_CHANGE;
    return new ComparisonItem(
        null,
        ConditionFieldCode.REPAYMENT_METHOD.name(),
        true,
        status,
        v1.repaymentMethod(),
        v2.repaymentMethod(),
        null,
        null,
        "ENUM",
        null,
        SOURCE_STRUCTURED_API,
        null);
  }

  private ComparisonItem compareFixedUpfrontFees(ConditionFields v1, ConditionFields v2) {
    long v1Fee = v1.stampTax() + v1.fixedUpfrontFees();
    long v2Fee = v2.stampTax() + v2.fixedUpfrontFees();
    ItemStatus status =
        v1Fee == v2Fee ? ItemStatus.SAME : (v2Fee < v1Fee ? ItemStatus.BETTER : ItemStatus.WORSE);
    return new ComparisonItem(
        null,
        ConditionFieldCode.FIXED_UPFRONT_FEES.name(),
        true,
        status,
        formatWon(v1Fee),
        formatWon(v2Fee),
        BigDecimal.valueOf(v1Fee),
        BigDecimal.valueOf(v2Fee),
        "WON",
        BigDecimal.valueOf(v2Fee - v1Fee),
        SOURCE_STRUCTURED_API,
        null);
  }

  private String joinLabels(ConditionFields fields) {
    return fields.preferentialConditions().stream()
        .map(PreferentialCondition::label)
        .collect(Collectors.joining(", "));
  }

  private String formatWon(long amount) {
    return "%,d원".formatted(amount);
  }

  private String formatRate(BigDecimal rate) {
    return "연 " + rate.toPlainString() + "%";
  }
}
