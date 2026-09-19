package com.im.server.condition.savepostconditionsnapshot;

import io.swagger.v3.oas.annotations.media.Schema;

public record SavePostConditionResponse(
    @Schema(description = "생성된 V2 Snapshot ID") Long postSnapshotId,
    @Schema(description = "매칭된 V1 Snapshot ID") Long preSnapshotId,
    @Schema(description = "자동 생성된 비교 Job ID") Long comparisonId) {}
