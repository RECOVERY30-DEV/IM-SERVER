package com.im.server.proof.internal;

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

/**
 * {@link MockBlockchainAdapter}가 쓰는 "원장" 저장소. 실제 체인이 정해지면 이 엔티티·리포지토리·어댑터 구현체만 통째로 교체하면 된다 — proof
 * 모듈의 다른 코드는 {@code proof.api.BlockchainAdapter}만 안다.
 */
@Entity
@Table(name = "proof_mock_ledger")
@Getter
@Setter
@NoArgsConstructor
public class MockLedgerEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "record_id_hash", nullable = false, unique = true, length = 64)
  private String recordIdHash;

  @Column(name = "payload_hash", nullable = false, length = 64)
  private String payloadHash;

  @Column(name = "schema_version", nullable = false, length = 10)
  private String schemaVersion;

  @Column(name = "issuer_id", nullable = false, length = 64)
  private String issuerId;

  @Column(name = "supersedes_record_id_hash", length = 64)
  private String supersedesRecordIdHash;

  @Column(name = "ledger_reference", nullable = false, unique = true, length = 128)
  private String ledgerReference;

  @Column(name = "confirmation_count", nullable = false)
  private int confirmationCount;

  @Column(name = "recorded_at", nullable = false)
  private Instant recordedAt;

  public MockLedgerEntry(
      String recordIdHash,
      String payloadHash,
      String schemaVersion,
      String issuerId,
      String supersedesRecordIdHash,
      String ledgerReference) {
    this.recordIdHash = recordIdHash;
    this.payloadHash = payloadHash;
    this.schemaVersion = schemaVersion;
    this.issuerId = issuerId;
    this.supersedesRecordIdHash = supersedesRecordIdHash;
    this.ledgerReference = ledgerReference;
    this.confirmationCount = 1;
    this.recordedAt = Instant.now();
  }
}
