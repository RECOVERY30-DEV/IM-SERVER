package com.im.server.condition.savepostconditionsnapshot;

import com.im.server.comparison.api.ComparisonApi;
import com.im.server.condition.domain.PostConditionSnapshot;
import com.im.server.condition.domain.PreConditionSnapshot;
import com.im.server.condition.internal.PostConditionSnapshotRepository;
import com.im.server.condition.internal.PreConditionSnapshotRepository;
import com.im.server.shared.exception.BusinessException;
import com.im.server.shared.exception.ErrorCode;
import com.im.server.shared.response.ApiResponse;
import com.im.server.shared.util.CanonicalJson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

/** FR02 — 최종조건(V2)을 봉인하고, 저장 즉시 비교(comparison)를 동기 실행한다. */
@RestController
@Tag(name = "condition", description = "사전조건(V1)/최종조건(V2) Snapshot")
public class SavePostConditionHandler {

  private final PreConditionSnapshotRepository preConditionSnapshotRepository;
  private final PostConditionSnapshotRepository postConditionSnapshotRepository;
  private final ComparisonApi comparisonApi;
  private final ObjectMapper objectMapper;

  public SavePostConditionHandler(
      PreConditionSnapshotRepository preConditionSnapshotRepository,
      PostConditionSnapshotRepository postConditionSnapshotRepository,
      ComparisonApi comparisonApi,
      ObjectMapper objectMapper) {
    this.preConditionSnapshotRepository = preConditionSnapshotRepository;
    this.postConditionSnapshotRepository = postConditionSnapshotRepository;
    this.comparisonApi = comparisonApi;
    this.objectMapper = objectMapper;
  }

  @Operation(
      summary = "최종조건(V2) 저장",
      description = "신청 시점에 저장된 V1과 매칭해서 봉인하고, 곧바로 비교 Job을 실행해 comparisonId를 반환한다.")
  @PostMapping("/api/applications/{applicationId}/post-conditions")
  public ResponseEntity<ApiResponse<SavePostConditionResponse>> handle(
      @PathVariable String applicationId, @RequestBody SavePostConditionCommand command) {

    PreConditionSnapshot preSnapshot =
        preConditionSnapshotRepository
            .findFirstByApplicationIdOrderByCreatedAtDesc(applicationId)
            .orElseThrow(
                () -> new BusinessException(ErrorCode.CONDITION_PRE_SNAPSHOT_NOT_FOUND_FOR_POST));

    postConditionSnapshotRepository
        .findByContractDocumentIdAndContractDocumentHash(
            command.contractDocumentId(), command.contractDocumentHash())
        .ifPresent(
            existing -> {
              throw new BusinessException(ErrorCode.CONDITION_POST_SNAPSHOT_DUPLICATE);
            });

    String payloadJson = writeJson(command.conditions());
    String payloadHash = CanonicalJson.hashOf(command.conditions());

    PostConditionSnapshot postSnapshot =
        new PostConditionSnapshot(
            applicationId,
            preSnapshot.getId(),
            command.reviewVersion(),
            command.contractDocumentId(),
            command.contractDocumentHash(),
            payloadJson,
            payloadHash,
            command.decidedAt());
    postConditionSnapshotRepository.save(postSnapshot);

    Long comparisonId =
        comparisonApi.startComparison(applicationId, preSnapshot.getId(), postSnapshot.getId());

    return ResponseEntity.ok(
        ApiResponse.success(
            new SavePostConditionResponse(
                postSnapshot.getId(), preSnapshot.getId(), comparisonId)));
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.CONDITION_INVALID_FIELDS, "조건 값을 직렬화할 수 없습니다");
    }
  }
}
