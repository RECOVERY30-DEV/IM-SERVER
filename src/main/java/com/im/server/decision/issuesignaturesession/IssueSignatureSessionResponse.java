package com.im.server.decision.issuesignaturesession;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record IssueSignatureSessionResponse(
    @Schema(description = "서명 세션 ID") String sessionId,
    @Schema(description = "세션 유효시간 만료 시각") Instant validUntil) {}
