package com.im.server.decision.domain;

import com.im.server.shared.exception.BusinessException;
import com.im.server.shared.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** FR12 — 진행/재검토/상담 결정과 전자서명. */
@Entity
@Table(name = "decisions")
@Getter
@Setter
@NoArgsConstructor
public class Decision {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "comparison_run_id", nullable = false, unique = true)
  private Long comparisonRunId;

  @Enumerated(EnumType.STRING)
  @Column(name = "decision_type", nullable = false, length = 15)
  private DecisionType decisionType;

  @Column(name = "bypassed_uncertain_items", nullable = false)
  private boolean bypassedUncertainItems;

  @Column(name = "all_items_confirmed", nullable = false)
  private boolean allItemsConfirmed;

  @Column(name = "signature_method", nullable = false, length = 20)
  private String signatureMethod;

  @Column(name = "signed_at")
  private Instant signedAt;

  @Column(name = "signature_valid_until")
  private Instant signatureValidUntil;

  @Column(name = "post_snapshot_id_at_decision", nullable = false)
  private Long postSnapshotIdAtDecision;

  @Column(name = "contract_document_hash_at_decision", nullable = false, length = 64)
  private String contractDocumentHashAtDecision;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public Decision(
      Long comparisonRunId,
      DecisionType decisionType,
      boolean bypassedUncertainItems,
      boolean allItemsConfirmed,
      Long postSnapshotIdAtDecision,
      String contractDocumentHashAtDecision) {
    if (decisionType == DecisionType.PROCEED && !allItemsConfirmed) {
      throw new BusinessException(ErrorCode.DECISION_REVIEW_GATE_NOT_CLEARED);
    }
    this.comparisonRunId = comparisonRunId;
    this.decisionType = decisionType;
    this.bypassedUncertainItems = bypassedUncertainItems;
    this.allItemsConfirmed = allItemsConfirmed;
    this.signatureMethod = "ELECTRONIC";
    this.signedAt = decisionType == DecisionType.PROCEED ? Instant.now() : null;
    this.postSnapshotIdAtDecision = postSnapshotIdAtDecision;
    this.contractDocumentHashAtDecision = contractDocumentHashAtDecision;
    this.createdAt = Instant.now();
  }
}
