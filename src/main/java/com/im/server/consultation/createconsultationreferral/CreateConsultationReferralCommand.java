package com.im.server.consultation.createconsultationreferral;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateConsultationReferralCommand(
    @Schema(description = "상담원에게 사전 맥락 전달 동의 여부 — 미동의여도 예약 자체는 성립")
        boolean transferConsentGranted) {}
