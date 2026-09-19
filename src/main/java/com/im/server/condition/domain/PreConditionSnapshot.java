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

/** V1(사전조건) Snapshot. 생성 후에는 어떤 필드도 수정하지 않는다 — 재조회는 항상 새 row. */
@Entity
@Table(name = "condition_pre_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class PreConditionSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "application_id", nullable = false)
  private String applicationId;

  @Column(name = "customer_id", nullable = false)
  private String customerId;

  @Column(name = "product_code", nullable = false)
  private String productCode;

  @Column(name = "product_version", nullable = false)
  private String productVersion;

  @Column(name = "idempotency_key", nullable = false, unique = true)
  private String idempotencyKey;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "snapshot_payload", nullable = false, columnDefinition = "json")
  private String snapshotPayload;

  @Column(name = "payload_hash", nullable = false, length = 64)
  private String payloadHash;

  @Column(name = "inquired_at", nullable = false)
  private Instant inquiredAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public PreConditionSnapshot(
      String applicationId,
      String customerId,
      String productCode,
      String productVersion,
      String idempotencyKey,
      String snapshotPayload,
      String payloadHash,
      Instant inquiredAt,
      Instant expiresAt) {
    if (isBlank(applicationId)
        || isBlank(customerId)
        || isBlank(productCode)
        || isBlank(productVersion)
        || isBlank(idempotencyKey)) {
      throw new BusinessException(ErrorCode.CONDITION_INVALID_FIELDS, "필수 식별자가 비어있습니다");
    }
    if (expiresAt == null || inquiredAt == null || !expiresAt.isAfter(inquiredAt)) {
      throw new BusinessException(ErrorCode.CONDITION_INVALID_FIELDS, "유효기간은 조회 시각 이후여야 합니다");
    }
    this.applicationId = applicationId;
    this.customerId = customerId;
    this.productCode = productCode;
    this.productVersion = productVersion;
    this.idempotencyKey = idempotencyKey;
    this.snapshotPayload = snapshotPayload;
    this.payloadHash = payloadHash;
    this.inquiredAt = inquiredAt;
    this.expiresAt = expiresAt;
    this.createdAt = Instant.now();
  }

  public boolean isExpired(Instant now) {
    return now.isAfter(expiresAt);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
