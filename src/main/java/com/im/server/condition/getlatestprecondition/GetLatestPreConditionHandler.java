package com.im.server.condition.getlatestprecondition;

import com.im.server.condition.api.ConditionFields;
import com.im.server.condition.domain.PreConditionSnapshot;
import com.im.server.condition.internal.PreConditionSnapshotRepository;
import com.im.server.shared.exception.BusinessException;
import com.im.server.shared.exception.ErrorCode;
import com.im.server.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

/** S01(사전조건 저장), E01(사전조건 유효기간 만료) 공용 조회. */
@RestController
@Tag(name = "condition", description = "사전조건(V1)/최종조건(V2) Snapshot")
public class GetLatestPreConditionHandler {

  private final PreConditionSnapshotRepository preConditionSnapshotRepository;
  private final ObjectMapper objectMapper;

  public GetLatestPreConditionHandler(
      PreConditionSnapshotRepository preConditionSnapshotRepository, ObjectMapper objectMapper) {
    this.preConditionSnapshotRepository = preConditionSnapshotRepository;
    this.objectMapper = objectMapper;
  }

  @Operation(summary = "최신 사전조건(V1) 조회", description = "가장 최근 V1과 유효 여부(VALID/EXPIRED)를 반환한다.")
  @GetMapping("/api/applications/{applicationId}/pre-conditions/latest")
  public ResponseEntity<ApiResponse<LatestPreConditionResponse>> handle(
      @PathVariable String applicationId) {
    PreConditionSnapshot snapshot =
        preConditionSnapshotRepository
            .findFirstByApplicationIdOrderByCreatedAtDesc(applicationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CONDITION_PRE_SNAPSHOT_NOT_FOUND));

    String status = snapshot.isExpired(Instant.now()) ? "EXPIRED" : "VALID";
    ConditionFields fields = readFields(snapshot.getSnapshotPayload());

    return ResponseEntity.ok(
        ApiResponse.success(
            new LatestPreConditionResponse(
                snapshot.getId(),
                status,
                snapshot.getInquiredAt(),
                snapshot.getExpiresAt(),
                fields)));
  }

  private ConditionFields readFields(String json) {
    try {
      return objectMapper.readValue(json, ConditionFields.class);
    } catch (Exception e) {
      throw new IllegalStateException("저장된 조건 payload를 읽을 수 없습니다", e);
    }
  }
}
