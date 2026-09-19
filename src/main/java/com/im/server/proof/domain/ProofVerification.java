package com.im.server.proof.domain;

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

/** implementation-spec.md 13.7 verify() 호출 이력. */
@Entity
@Table(name = "proof_verifications")
@Getter
@Setter
@NoArgsConstructor
public class ProofVerification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "proof_record_id", nullable = false)
  private Long proofRecordId;

  @Column(name = "requested_by", nullable = false, length = 20)
  private String requestedBy;

  @Column(name = "recomputed_hash", nullable = false, length = 64)
  private String recomputedHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "verify_result", nullable = false, length = 20)
  private VerifyResult verifyResult;

  @Column(name = "ledger_recorded_at")
  private Instant ledgerRecordedAt;

  @Column(name = "verified_at", nullable = false)
  private Instant verifiedAt;

  public ProofVerification(
      Long proofRecordId,
      String requestedBy,
      String recomputedHash,
      VerifyResult verifyResult,
      Instant ledgerRecordedAt) {
    this.proofRecordId = proofRecordId;
    this.requestedBy = requestedBy;
    this.recomputedHash = recomputedHash;
    this.verifyResult = verifyResult;
    this.ledgerRecordedAt = ledgerRecordedAt;
    this.verifiedAt = Instant.now();
  }
}
