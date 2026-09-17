package com.im.server.condition.listpreconditions;

import com.im.server.condition.api.ConditionFields;
import com.im.server.condition.domain.PreConditionSnapshot;
import com.im.server.condition.internal.PreConditionSnapshotRepository;
import com.im.server.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

/** E01 "이전 조회 결과 보기" — 과거 V1 이력 요약. */
@RestController
@Tag(name = "condition", description = "사전조건(V1)/최종조건(V2) Snapshot")
public class ListPreConditionsHandler {

  private final PreConditionSnapshotRepository preConditionSnapshotRepository;
  private final ObjectMapper objectMapper;

  public ListPreConditionsHandler(
      PreConditionSnapshotRepository preConditionSnapshotRepository, ObjectMapper objectMapper) {
    this.preConditionSnapshotRepository = preConditionSnapshotRepository;
    this.objectMapper = objectMapper;
  }

  @Operation(summary = "사전조건(V1) 조회 이력", description = "신청건에 대해 지금까지 조회된 V1 목록을 최신순으로 반환한다.")
  @GetMapping("/api/applications/{applicationId}/pre-conditions")
  public ResponseEntity<ApiResponse<List<PreConditionSummaryResponse>>> handle(
      @PathVariable String applicationId) {
    List<PreConditionSummaryResponse> items =
        preConditionSnapshotRepository
            .findByApplicationIdOrderByCreatedAtDesc(applicationId)
            .stream()
            .map(this::toSummary)
            .toList();
    return ResponseEntity.ok(ApiResponse.success(items));
  }

  private PreConditionSummaryResponse toSummary(PreConditionSnapshot snapshot) {
    ConditionFields fields;
    try {
      fields = objectMapper.readValue(snapshot.getSnapshotPayload(), ConditionFields.class);
    } catch (Exception e) {
      throw new IllegalStateException("저장된 조건 payload를 읽을 수 없습니다", e);
    }
    return new PreConditionSummaryResponse(
        snapshot.getId(),
        snapshot.getInquiredAt(),
        snapshot.getExpiresAt(),
        fields.loanAmount(),
        fields.finalRatePercent());
  }
}
