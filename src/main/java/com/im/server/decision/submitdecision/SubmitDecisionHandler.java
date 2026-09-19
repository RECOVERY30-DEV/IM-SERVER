package com.im.server.decision.submitdecision;

import com.im.server.comparison.api.ComparisonApi;
import com.im.server.comparison.api.ComparisonItemView;
import com.im.server.comparison.api.ComparisonRunStatus;
import com.im.server.comparison.api.ComparisonRunView;
import com.im.server.condition.api.ConditionApi;
import com.im.server.decision.domain.Decision;
import com.im.server.decision.domain.DecisionRequiredReview;
import com.im.server.decision.domain.DecisionType;
import com.im.server.decision.internal.DecisionRepository;
import com.im.server.decision.internal.DecisionRequiredReviewRepository;
import com.im.server.shared.exception.BusinessException;
import com.im.server.shared.exception.ErrorCode;
import com.im.server.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR12 — S05 "현재 조건으로 약정하기" / "다시 검토", S03 UNCERTAIN 변형의 "기존 절차로 계속".
 *
 * <p>{@code bypassedUncertainItems}가 true여도 이 구현은 필수 확인 게이트를 완화하지 않는다 — "UNCERTAIN 항목이 있어도 기존 절차로
 * 넘어간다"는 의미가 게이트 자체를 건너뛴다는 뜻인지, 그저 감사 로그에 남기는 사실 플래그인지가 아직 기획 확정 사항이 아니라서(`docs/api-design.md`
 * 10.1) 안전한 쪽(게이트 유지)으로 구현했다.
 */
@RestController
@Tag(name = "decision", description = "Review Gate / 약정 결정 / 전자서명")
public class SubmitDecisionHandler {

  private final ComparisonApi comparisonApi;
  private final ConditionApi conditionApi;
  private final DecisionRepository decisionRepository;
  private final DecisionRequiredReviewRepository decisionRequiredReviewRepository;

  public SubmitDecisionHandler(
      ComparisonApi comparisonApi,
      ConditionApi conditionApi,
      DecisionRepository decisionRepository,
      DecisionRequiredReviewRepository decisionRequiredReviewRepository) {
    this.comparisonApi = comparisonApi;
    this.conditionApi = conditionApi;
    this.decisionRepository = decisionRepository;
    this.decisionRequiredReviewRepository = decisionRequiredReviewRepository;
  }

  @Operation(summary = "약정 결정 저장", description = "PROCEED는 필수 확인이 모두 끝나야 저장할 수 있다.")
  @PostMapping("/api/comparisons/{comparisonId}/decisions")
  public ResponseEntity<ApiResponse<SubmitDecisionResponse>> handle(
      @PathVariable Long comparisonId, @RequestBody SubmitDecisionCommand command) {

    ComparisonRunView run = comparisonApi.getRun(comparisonId);
    if (run.status() != ComparisonRunStatus.COMPLETED) {
      throw new BusinessException(ErrorCode.COMPARISON_RUN_NOT_COMPLETED);
    }
    decisionRepository
        .findByComparisonRunId(comparisonId)
        .ifPresent(
            existing -> {
              throw new BusinessException(ErrorCode.DECISION_ALREADY_SUBMITTED);
            });

    boolean allReviewed = isAllReviewed(comparisonId);
    if (command.decisionType() == DecisionType.PROCEED && !allReviewed) {
      throw new BusinessException(ErrorCode.DECISION_REVIEW_GATE_NOT_CLEARED);
    }

    String contractDocumentHash =
        conditionApi.getPostSnapshot(run.postSnapshotId()).contractDocumentHash();

    Decision decision =
        new Decision(
            comparisonId,
            command.decisionType(),
            command.bypassedUncertainItems(),
            allReviewed,
            run.postSnapshotId(),
            contractDocumentHash);
    decisionRepository.save(decision);

    return ResponseEntity.ok(
        ApiResponse.success(
            new SubmitDecisionResponse(
                decision.getId(), decision.getDecisionType(), decision.getSignedAt())));
  }

  private boolean isAllReviewed(Long comparisonId) {
    List<ComparisonItemView> requiredItems = comparisonApi.listRequiredReviewItems(comparisonId);
    if (requiredItems.isEmpty()) {
      return true;
    }
    Set<Long> reviewedItemIds =
        decisionRequiredReviewRepository.findByComparisonRunId(comparisonId).stream()
            .filter(DecisionRequiredReview::isReviewed)
            .map(DecisionRequiredReview::getComparisonItemId)
            .collect(Collectors.toSet());
    return requiredItems.stream().map(ComparisonItemView::id).allMatch(reviewedItemIds::contains);
  }
}
