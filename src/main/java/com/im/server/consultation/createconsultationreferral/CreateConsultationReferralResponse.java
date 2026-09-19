package com.im.server.consultation.createconsultationreferral;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateConsultationReferralResponse(
    @Schema(description = "핸드오프 기록 ID") Long referralId,
    @Schema(description = "전송 동의 여부") boolean transferConsentGranted,
    @Schema(description = "외부 상담 시스템 예약 ID (연동 전이라 항상 null)") String externalConsultationRef) {}
