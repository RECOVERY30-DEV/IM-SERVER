package com.im.server.decision.submitdecision;

import com.im.server.decision.api.DecisionType;
import io.swagger.v3.oas.annotations.media.Schema;

public record SubmitDecisionCommand(
    @Schema(description = "PROCEED, RECONSIDER, CONSULT 중 하나") DecisionType decisionType,
    @Schema(description = "PROCEED일 때 필수 — signature-sessions에서 발급받은 세션 ID")
        String signatureSessionId,
    @Schema(description = "UNCERTAIN 항목이 남아있는 채로 진행했는지 (S03 '기존 절차로 계속')")
        boolean bypassedUncertainItems) {}
