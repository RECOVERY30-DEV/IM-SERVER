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

/** S05 "전자서명" 유효시간 카운트다운. Decision 저장 시 이 세션이 유효한지 검증한다. */
@Entity
@Table(name = "decision_signature_sessions")
@Getter
@Setter
@NoArgsConstructor
public class SignatureSession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "comparison_run_id", nullable = false)
  private Long comparisonRunId;

  @Column(name = "session_id", nullable = false, unique = true, length = 64)
  private String sessionId;

  @Column(name = "valid_until", nullable = false)
  private Instant validUntil;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public SignatureSession(Long comparisonRunId, String sessionId, Instant validUntil) {
    this.comparisonRunId = comparisonRunId;
    this.sessionId = sessionId;
    this.validUntil = validUntil;
    this.createdAt = Instant.now();
  }

  public boolean isUsable(Long expectedComparisonRunId, Instant now) {
    return usedAt == null
        && now.isBefore(validUntil)
        && comparisonRunId.equals(expectedComparisonRunId);
  }

  public void markUsed() {
    this.usedAt = Instant.now();
  }
}
