package com.im.server.proof.getproofstatus;

import com.im.server.comparison.api.ComparisonApi;
import com.im.server.comparison.api.ItemStatus;
import com.im.server.condition.api.ConditionApi;
import com.im.server.condition.api.PostConditionSnapshotView;
import com.im.server.decision.api.DecisionApi;
import com.im.server.decision.api.DecisionView;
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

/** S06 상단 요약 + "검증됨" 배지. PROCEED 결정에만 Proof가 존재한다. */
@RestController
@Tag(name = "proof", description = "무결성 증빙 (Blockchain Adapter)")
public class GetProofStatusHandler {

  private final DecisionApi decisionApi;
  private final ComparisonApi comparisonApi;
  private final ConditionApi conditionApi;
  private final ProofRecordRepository proofRecordRepository;

  public GetProofStatusHandler(
      DecisionApi decisionApi,
      ComparisonApi comparisonApi,
      ConditionApi conditionApi,
      ProofRecordRepository proofRecordRepository) {
    this.decisionApi = decisionApi;
    this.comparisonApi = comparisonApi;
    this.conditionApi = conditionApi;
    this.proofRecordRepository = proofRecordRepository;
  }

  @Operation(summary = "약정 완료 요약 + 증빙 상태 조회")
  @GetMapping("/api/decisions/{decisionId}/proof")
  public ResponseEntity<ApiResponse<ProofStatusResponse>> handle(@PathVariable Long decisionId) {
    DecisionView decision = decisionApi.getDecision(decisionId);
    ProofRecord proofRecord =
        proofRecordRepository
            .findByDecisionId(decisionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROOF_NOT_FOUND));

    PostConditionSnapshotView postSnapshot =
        conditionApi.getPostSnapshot(decision.postSnapshotIdAtDecision());

    var items = comparisonApi.listAllItems(decision.comparisonRunId());
    int changedItemsCount =
        (int) items.stream().filter(i -> i.itemStatus() != ItemStatus.SAME).count();

    ProofRecordSummaryResponse summary =
        new ProofRecordSummaryResponse(changedItemsCount, decision.allItemsConfirmed(), false);

    return ResponseEntity.ok(
        ApiResponse.success(
            new ProofStatusResponse(
                proofRecord.getProofId(),
                postSnapshot.contractDocumentId(),
                postSnapshot.fields().loanAmount(),
                postSnapshot.fields().finalRatePercent(),
                decision.signedAt(),
                proofRecord.getAnchorStatus(),
                summary)));
  }
}
