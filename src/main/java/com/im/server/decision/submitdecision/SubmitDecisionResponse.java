package com.im.server.decision.submitdecision;

import com.im.server.decision.domain.DecisionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record SubmitDecisionResponse(
    @Schema(description = "생성된 Decision ID") Long decisionId,
    @Schema(description = "결정 유형") DecisionType decisionType,
    @Schema(description = "전자서명 시각(PROCEED만)") Instant signedAt) {}
