package com.im.server.condition.savepreconditionsnapshot;

import com.im.server.condition.domain.PreConditionSnapshot;
import com.im.server.condition.internal.PreConditionSnapshotRepository;
import com.im.server.shared.exception.BusinessException;
import com.im.server.shared.exception.ErrorCode;
import com.im.server.shared.response.ApiResponse;
import com.im.server.shared.util.CanonicalJson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

/** FR01 — 사전조건(V1)을 변경 불가능한 Snapshot으로 봉인한다. */
@RestController
@Tag(name = "condition", description = "사전조건(V1)/최종조건(V2) Snapshot")
public class SavePreConditionHandler {

  private final PreConditionSnapshotRepository preConditionSnapshotRepository;
  private final ObjectMapper objectMapper;

  public SavePreConditionHandler(
      PreConditionSnapshotRepository preConditionSnapshotRepository, ObjectMapper objectMapper) {
    this.preConditionSnapshotRepository = preConditionSnapshotRepository;
    this.objectMapper = objectMapper;
  }

  @Operation(
      summary = "사전조건(V1) 저장",
      description = "동일 Idempotency-Key로 재호출하면 새로 만들지 않고 기존 Snapshot을 그대로 반환한다.")
  @PostMapping("/api/applications/{applicationId}/pre-conditions")
  public ResponseEntity<ApiResponse<SavePreConditionResponse>> handle(
      @PathVariable String applicationId,
      @Parameter(description = "재호출 시 동일 Snapshot을 보장하는 키") @RequestHeader("Idempotency-Key")
          String idempotencyKey,
      @RequestBody SavePreConditionCommand command) {

    String payloadHash = CanonicalJson.hashOf(command.conditions());

    Optional<PreConditionSnapshot> existing =
        preConditionSnapshotRepository.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
      PreConditionSnapshot snapshot = existing.get();
      if (!snapshot.getPayloadHash().equals(payloadHash)) {
        throw new BusinessException(ErrorCode.CONDITION_IDEMPOTENCY_KEY_CONFLICT);
      }
      return ResponseEntity.ok(ApiResponse.success(toResponse(snapshot, command)));
    }

    String payloadJson = writeJson(command.conditions());
    PreConditionSnapshot snapshot =
        new PreConditionSnapshot(
            applicationId,
            command.customerId(),
            command.productCode(),
            command.productVersion(),
            idempotencyKey,
            payloadJson,
            payloadHash,
            command.inquiredAt(),
            command.expiresAt());
    preConditionSnapshotRepository.save(snapshot);

    return ResponseEntity.ok(ApiResponse.success(toResponse(snapshot, command)));
  }

  private SavePreConditionResponse toResponse(
      PreConditionSnapshot snapshot, SavePreConditionCommand command) {
    return new SavePreConditionResponse(
        snapshot.getId(),
        snapshot.getApplicationId(),
        snapshot.getExpiresAt(),
        snapshot.getPayloadHash(),
        command.conditions());
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.CONDITION_INVALID_FIELDS, "조건 값을 직렬화할 수 없습니다");
    }
  }
}
