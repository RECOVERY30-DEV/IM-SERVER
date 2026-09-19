package com.im.server.proof.getproofdetail;

import com.im.server.proof.domain.ProofRecord;
import com.im.server.proof.internal.ProofRecordRepository;
import com.im.server.shared.exception.BusinessException;
import com.im.server.shared.exception.ErrorCode;
import com.im.server.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** S06 "기록 자세히 보기" — Canonical Payload 필드와 anchor 상세. */
@RestController
@Tag(name = "proof", description = "무결성 증빙 (Blockchain Adapter)")
public class GetProofDetailHandler {

  private final ProofRecordRepository proofRecordRepository;

  public GetProofDetailHandler(ProofRecordRepository proofRecordRepository) {
    this.proofRecordRepository = proofRecordRepository;
  }

  @Operation(summary = "Proof 상세 조회")
  @GetMapping("/api/proof/{proofId}")
  public ResponseEntity<ApiResponse<ProofDetailResponse>> handle(@PathVariable String proofId) {
    ProofRecord record =
        proofRecordRepository
            .findByProofId(proofId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROOF_NOT_FOUND));

    return ResponseEntity.ok(ApiResponse.success(toResponse(record)));
  }

  static ProofDetailResponse toResponse(ProofRecord record) {
    return new ProofDetailResponse(
        record.getProofId(),
        record.getSchemaVersion(),
        record.getCheckIdHash(),
        record.getApplicationIdHash(),
        record.getV1SnapshotHash(),
        record.getV2SnapshotHash(),
        record.getComparisonResultHash(),
        record.getDecisionHash(),
        record.getIssuerId(),
        record.getPromptVersion(),
        record.getRuleVersion(),
        record.getCalculationVersion(),
        record.getPayloadHash(),
        record.getAnchorStatus(),
        record.getLedgerReference(),
        record.getConfirmationCount(),
        record.getSubmittedAt(),
        record.getConfirmedAt(),
        record.getAnchorRetryCount());
  }
}
