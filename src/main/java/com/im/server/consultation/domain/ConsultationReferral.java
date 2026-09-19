package com.im.server.consultation.domain;

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
 * S03/S03-UNCERTAIN "상담원에게 문의" 핸드오프 기록. 실제 상담 예약·채팅 자체는 Non-goal(PRD 5.2, ProofTalk 통합 제외)이라 이 엔티티는
 * 요청 사실과 동의 여부만 남긴다.
 */
@Entity
@Table(name = "consultation_referrals")
@Getter
@Setter
@NoArgsConstructor
public class ConsultationReferral {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "comparison_run_id", nullable = false)
  private Long comparisonRunId;

  @Column(name = "application_id", nullable = false)
  private String applicationId;

  @Column(name = "transfer_consent_granted", nullable = false)
  private boolean transferConsentGranted;

  @Column(name = "external_consultation_ref", length = 64)
  private String externalConsultationRef;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  public ConsultationReferral(
      Long comparisonRunId, String applicationId, boolean transferConsentGranted) {
    this.comparisonRunId = comparisonRunId;
    this.applicationId = applicationId;
    this.transferConsentGranted = transferConsentGranted;
    this.requestedAt = Instant.now();
  }
}
