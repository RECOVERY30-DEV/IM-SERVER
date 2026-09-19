package com.im.server.comparison.internal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 원리금균등 상환 계산기 (FR08). 매 회차 이자를 잔액 기준으로 계산해서 더하고, 마지막 회차에 남은 잔액을 그대로 상환하도록 시뮬레이션한다 — Release
 * Acceptance Criteria "마지막 회차 잔액이 0원"을 알고리즘으로 보장한다.
 */
public final class EqualInstallmentCalculator {

  private static final MathContext MC = new MathContext(20);

  private EqualInstallmentCalculator() {}

  public record AmortizationResult(BigDecimal monthlyPayment, BigDecimal totalInterest) {}

  public static AmortizationResult calculate(
      long principal, BigDecimal annualRatePercent, int termMonths) {
    if (principal <= 0 || termMonths <= 0) {
      throw new IllegalArgumentException("원금과 기간은 0보다 커야 합니다");
    }

    BigDecimal monthlyRate = annualRatePercent.divide(BigDecimal.valueOf(1200), MC);

    BigDecimal levelPayment;
    if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
      levelPayment =
          BigDecimal.valueOf(principal)
              .divide(BigDecimal.valueOf(termMonths), 0, RoundingMode.HALF_UP);
    } else {
      BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
      BigDecimal factor = onePlusR.pow(termMonths, MC);
      BigDecimal numerator = BigDecimal.valueOf(principal).multiply(monthlyRate).multiply(factor);
      BigDecimal denominator = factor.subtract(BigDecimal.ONE);
      levelPayment = numerator.divide(denominator, MC).setScale(0, RoundingMode.HALF_UP);
    }

    BigDecimal balance = BigDecimal.valueOf(principal);
    BigDecimal totalInterest = BigDecimal.ZERO;
    for (int month = 1; month <= termMonths; month++) {
      BigDecimal interest = balance.multiply(monthlyRate, MC).setScale(0, RoundingMode.HALF_UP);
      totalInterest = totalInterest.add(interest);
      if (month == termMonths) {
        balance = BigDecimal.ZERO;
      } else {
        balance = balance.subtract(levelPayment.subtract(interest));
      }
    }
    return new AmortizationResult(levelPayment, totalInterest);
  }
}
