package com.im.server.comparison.internal;

import com.im.server.comparison.domain.ComparisonItem;
import com.im.server.condition.api.ConditionFieldCode;
import java.math.BigDecimal;

/** comparison_items를 화면/응답에 맞게 표시 문자열로 바꾸는 공용 로직 (comparison 모듈 내부 전용). */
public final class ComparisonItemPresenter {

  private ComparisonItemPresenter() {}

  public static String label(String fieldCode) {
    return ConditionFieldCode.valueOf(fieldCode).getLabel();
  }

  public static String deltaLabel(ComparisonItem item) {
    BigDecimal delta = item.getDeltaNumeric();
    if (delta == null) {
      return null;
    }
    if (delta.signum() == 0) {
      return "변경 없음";
    }
    String sign = delta.signum() > 0 ? "+" : "-";
    BigDecimal abs = delta.abs();
    return switch (item.getUnit()) {
      case "PERCENT" -> sign + abs.toPlainString() + "%p";
      case "WON" -> sign + "%,d원".formatted(abs.longValueExact());
      case "MONTHS" -> sign + abs.toBigInteger() + "개월";
      default -> sign + abs.toPlainString();
    };
  }
}
