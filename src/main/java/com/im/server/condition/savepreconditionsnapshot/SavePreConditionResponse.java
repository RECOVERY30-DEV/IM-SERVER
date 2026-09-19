package com.im.server.condition.savepreconditionsnapshot;

import com.im.server.condition.api.ConditionFields;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record SavePreConditionResponse(
    @Schema(description = "생성되거나 재사용된 V1 Snapshot ID") Long preSnapshotId,
    @Schema(description = "대출 신청 ID") String applicationId,
    @Schema(description = "V1 유효기간") Instant expiresAt,
    @Schema(description = "Payload의 SHA-256 Hash") String payloadHash,
    @Schema(description = "사전조건 값(echo)") ConditionFields conditions) {}
