package com.im.server.condition.domain;

import com.im.server.shared.exception.BusinessException;
import com.im.server.shared.exception.ErrorCode;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** V2(최종 약정조건) Snapshot. 계약서가 바뀌면 새 row를 만든다 — 기존 row는 수정하지 않는다. */
@Entity
@Table(name = "condition_post_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class PostConditionSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "application_id", nullable = false)
  private String applicationId;

  @Column(name = "pre_snapshot_id", nullable = false)
  private Long preSnapshotId;

  @Column(name = "review_version", nullable = false)
  private String reviewVersion;

  @Column(name = "contract_document_id", nullable = false)
  private String contractDocumentId;

  @Column(name = "contract_document_hash", nullable = false, length = 64)
  private String contractDocumentHash;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "snapshot_payload", nullable = false, columnDefinition = "json")
  private String snapshotPayload;

  @Column(name = "payload_hash", nullable = false, length = 64)
  private String payloadHash;

  @Column(name = "decided_at", nullable = false)
  private Instant decidedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public PostConditionSnapshot(
      String applicationId,
      Long preSnapshotId,
      String reviewVersion,
      String contractDocumentId,
      String contractDocumentHash,
      String snapshotPayload,
      String payloadHash,
      Instant decidedAt) {
    if (isBlank(applicationId) || preSnapshotId == null || isBlank(reviewVersion)) {
      throw new BusinessException(ErrorCode.CONDITION_INVALID_FIELDS, "필수 식별자가 비어있습니다");
    }
    if (isBlank(contractDocumentId) || isBlank(contractDocumentHash)) {
      throw new BusinessException(ErrorCode.CONDITION_INVALID_FIELDS, "계약서 식별자/Hash가 비어있습니다");
    }
    this.applicationId = applicationId;
    this.preSnapshotId = preSnapshotId;
    this.reviewVersion = reviewVersion;
    this.contractDocumentId = contractDocumentId;
    this.contractDocumentHash = contractDocumentHash;
    this.snapshotPayload = snapshotPayload;
    this.payloadHash = payloadHash;
    this.decidedAt = decidedAt == null ? Instant.now() : decidedAt;
    this.createdAt = Instant.now();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
