package com.im.server.decision.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** FR11 Review Gate — comparison_items 중 requiresReview인 항목 하나당 1행. */
@Entity
@Table(name = "decision_required_reviews")
@Getter
@Setter
@NoArgsConstructor
public class DecisionRequiredReview {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "comparison_run_id", nullable = false)
  private Long comparisonRunId;

  @Column(name = "comparison_item_id", nullable = false, unique = true)
  private Long comparisonItemId;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  public DecisionRequiredReview(Long comparisonRunId, Long comparisonItemId) {
    this.comparisonRunId = comparisonRunId;
    this.comparisonItemId = comparisonItemId;
  }

  public boolean isReviewed() {
    return reviewedAt != null;
  }

  public void markReviewed() {
    if (reviewedAt == null) {
      reviewedAt = Instant.now();
    }
  }
}
