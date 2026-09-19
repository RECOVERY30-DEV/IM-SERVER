package com.im.server.proof.domain;

import com.im.server.proof.api.AnchorStatus;
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

/** FR13 — implementation-spec.md 13.2 Canonical Proof Payload를 Off-chain에 보관하는 행. */
@Entity
@Table(name = "proof_records")
@Getter
@Setter
@NoArgsConstructor
public class ProofRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "decision_id", nullable = false, unique = true)
  private Long decisionId;

  @Column(name = "proof_id", nullable = false, unique = true, length = 64)
  private String proofId;

  @Column(name = "record_id_hash", nullable = false, unique = true, length = 64)
  private String recordIdHash;

  @Column(name = "schema_version", nullable = false, length = 10)
  private String schemaVersion;

  @Column(name = "check_id_hash", nullable = false, length = 64)
  private String checkIdHash;

  @Column(name = "application_id_hash", nullable = false, length = 64)
  private String applicationIdHash;

  @Column(name = "v1_snapshot_hash", nullable = false, length = 64)
  private String v1SnapshotHash;

  @Column(name = "v2_snapshot_hash", nullable = false, length = 64)
  private String v2SnapshotHash;

  @Column(name = "comparison_result_hash", nullable = false, length = 64)
  private String comparisonResultHash;

  @Column(name = "decision_hash", nullable = false, length = 64)
  private String decisionHash;

  @Column(name = "issuer_id", nullable = false, length = 64)
  private String issuerId;

  @Column(name = "prompt_version")
  private String promptVersion;

  @Column(name = "rule_version", nullable = false, length = 32)
  private String ruleVersion;

  @Column(name = "calculation_version", nullable = false, length = 32)
  private String calculationVersion;

  @Column(name = "payload_hash", nullable = false, length = 64)
  private String payloadHash;

  @Column(name = "supersedes_record_id_hash", length = 64)
  private String supersedesRecordIdHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "anchor_status", nullable = false, length = 15)
  private AnchorStatus anchorStatus;

  @Column(name = "ledger_reference", length = 128)
  private String ledgerReference;

  @Column(name = "confirmation_count", nullable = false)
  private int confirmationCount;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @Column(name = "anchor_retry_count", nullable = false)
  private short anchorRetryCount;

  @Column(name = "next_retry_at")
  private Instant nextRetryAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public ProofRecord(
      Long decisionId,
      String proofId,
      String recordIdHash,
      String schemaVersion,
      String checkIdHash,
      String applicationIdHash,
      String v1SnapshotHash,
      String v2SnapshotHash,
      String comparisonResultHash,
      String decisionHash,
      String issuerId,
      String promptVersion,
      String ruleVersion,
      String calculationVersion,
      String payloadHash,
      Instant createdAt) {
    this.decisionId = decisionId;
    this.proofId = proofId;
    this.recordIdHash = recordIdHash;
    this.schemaVersion = schemaVersion;
    this.checkIdHash = checkIdHash;
    this.applicationIdHash = applicationIdHash;
    this.v1SnapshotHash = v1SnapshotHash;
    this.v2SnapshotHash = v2SnapshotHash;
    this.comparisonResultHash = comparisonResultHash;
    this.decisionHash = decisionHash;
    this.issuerId = issuerId;
    this.promptVersion = promptVersion;
    this.ruleVersion = ruleVersion;
    this.calculationVersion = calculationVersion;
    this.payloadHash = payloadHash;
    this.anchorStatus = AnchorStatus.PENDING;
    this.confirmationCount = 0;
    this.anchorRetryCount = 0;
    // Canonical Payload에 넣은 createdAt과 정확히 같은 값이어야 한다 — verify() 재해싱 시 이 값을 그대로
    // 다시 쓰기 때문에(GetProofDetailHandler는 안 그렇지만 검증 로직은 이 필드로 payload를 재구성한다),
    // 여기서 새로 Instant.now()를 부르면 나노초 단위로 어긋나 항상 HASH_MISMATCH가 난다.
    this.createdAt = createdAt;
  }

  public void markSubmitted(String ledgerReference) {
    this.anchorStatus = AnchorStatus.SUBMITTED;
    this.ledgerReference = ledgerReference;
    this.submittedAt = Instant.now();
  }

  public void markConfirmed(int confirmationCount) {
    this.anchorStatus = AnchorStatus.CONFIRMED;
    this.confirmationCount = confirmationCount;
    this.confirmedAt = Instant.now();
  }

  public void markFailed() {
    this.anchorStatus = AnchorStatus.FAILED;
    this.anchorRetryCount = (short) (this.anchorRetryCount + 1);
  }
}
