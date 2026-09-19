package com.im.server.comparison.domain;

import com.im.server.comparison.api.ItemStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** FR07 — Field 하나에 대한 V1·V2 비교 결과. */
@Entity
@Table(name = "comparison_items")
@Getter
@Setter
@NoArgsConstructor
public class ComparisonItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "comparison_run_id", nullable = false)
  private Long comparisonRunId;

  @Column(name = "field_code", nullable = false, length = 40)
  private String fieldCode;

  @Column(name = "contributes_to_overall", nullable = false)
  private boolean contributesToOverall;

  @Enumerated(EnumType.STRING)
  @Column(name = "item_status", nullable = false, length = 20)
  private ItemStatus itemStatus;

  @Column(name = "v1_value_text")
  private String v1ValueText;

  @Column(name = "v2_value_text")
  private String v2ValueText;

  @Column(name = "v1_value_numeric")
  private BigDecimal v1ValueNumeric;

  @Column(name = "v2_value_numeric")
  private BigDecimal v2ValueNumeric;

  @Column(name = "unit", length = 20)
  private String unit;

  @Column(name = "delta_numeric")
  private BigDecimal deltaNumeric;

  @Column(name = "source", nullable = false, length = 15)
  private String source;

  @Column(name = "confidence")
  private BigDecimal confidence;

  @Column(name = "unknown_reason")
  private String unknownReason;

  @Column(name = "requires_review", nullable = false)
  private boolean requiresReview;

  public ComparisonItem(
      Long comparisonRunId,
      String fieldCode,
      boolean contributesToOverall,
      ItemStatus itemStatus,
      String v1ValueText,
      String v2ValueText,
      BigDecimal v1ValueNumeric,
      BigDecimal v2ValueNumeric,
      String unit,
      BigDecimal deltaNumeric,
      String source,
      String unknownReason) {
    this.comparisonRunId = comparisonRunId;
    this.fieldCode = fieldCode;
    this.contributesToOverall = contributesToOverall;
    this.itemStatus = itemStatus;
    this.v1ValueText = v1ValueText;
    this.v2ValueText = v2ValueText;
    this.v1ValueNumeric = v1ValueNumeric;
    this.v2ValueNumeric = v2ValueNumeric;
    this.unit = unit;
    this.deltaNumeric = deltaNumeric;
    this.source = source;
    this.unknownReason = unknownReason;
    this.requiresReview =
        contributesToOverall
            && (itemStatus == ItemStatus.WORSE
                || itemStatus == ItemStatus.STRUCTURAL_CHANGE
                || itemStatus == ItemStatus.UNKNOWN);
  }
}
