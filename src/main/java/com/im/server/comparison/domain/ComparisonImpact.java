package com.im.server.comparison.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** FR08/FR09 — 월 납입액·총이자·확정비용의 V1 대비 변화. run당 1행. */
@Entity
@Table(name = "comparison_impacts")
@Getter
@Setter
@NoArgsConstructor
public class ComparisonImpact {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "comparison_run_id", nullable = false, unique = true)
  private Long comparisonRunId;

  @Column(name = "v1_monthly_payment", nullable = false)
  private BigDecimal v1MonthlyPayment;

  @Column(name = "v2_monthly_payment", nullable = false)
  private BigDecimal v2MonthlyPayment;

  @Column(name = "monthly_payment_delta", nullable = false)
  private BigDecimal monthlyPaymentDelta;

  @Column(name = "v1_total_interest", nullable = false)
  private BigDecimal v1TotalInterest;

  @Column(name = "v2_total_interest", nullable = false)
  private BigDecimal v2TotalInterest;

  @Column(name = "total_interest_delta", nullable = false)
  private BigDecimal totalInterestDelta;

  @Column(name = "fixed_fee_delta", nullable = false)
  private BigDecimal fixedFeeDelta;

  @Column(name = "total_cost_delta", nullable = false)
  private BigDecimal totalCostDelta;

  @Column(name = "calculation_basis", nullable = false, length = 20)
  private String calculationBasis;

  @Column(name = "rounding_rule", nullable = false, length = 20)
  private String roundingRule;

  @Column(name = "calculated_at", nullable = false)
  private Instant calculatedAt;

  public ComparisonImpact(
      Long comparisonRunId,
      BigDecimal v1MonthlyPayment,
      BigDecimal v2MonthlyPayment,
      BigDecimal v1TotalInterest,
      BigDecimal v2TotalInterest,
      BigDecimal fixedFeeDelta,
      String calculationBasis) {
    this.comparisonRunId = comparisonRunId;
    this.v1MonthlyPayment = v1MonthlyPayment;
    this.v2MonthlyPayment = v2MonthlyPayment;
    this.monthlyPaymentDelta = v2MonthlyPayment.subtract(v1MonthlyPayment);
    this.v1TotalInterest = v1TotalInterest;
    this.v2TotalInterest = v2TotalInterest;
    this.totalInterestDelta = v2TotalInterest.subtract(v1TotalInterest);
    this.fixedFeeDelta = fixedFeeDelta;
    this.totalCostDelta = this.totalInterestDelta.add(fixedFeeDelta);
    this.calculationBasis = calculationBasis;
    this.roundingRule = "ROUND_HALF_UP";
    this.calculatedAt = Instant.now();
  }
}
