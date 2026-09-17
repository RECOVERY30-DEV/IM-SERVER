package com.im.server.condition.savepostconditionsnapshot;

import com.im.server.condition.api.ConditionFields;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record SavePostConditionCommand(
    @Schema(description = "최종 심사 Version", example = "review-2026-09-12") String reviewVersion,
    @Schema(description = "전자약정서 식별자") String contractDocumentId,
    @Schema(description = "전자약정서 원문 Hash") String contractDocumentHash,
    @Schema(description = "최종 심사 완료 시각") Instant decidedAt,
    @Schema(description = "최종조건 값") ConditionFields conditions) {}
