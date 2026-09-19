package com.im.server.decision.submitdecision;

import com.im.server.comparison.api.ComparisonApi;
import com.im.server.comparison.api.ComparisonRunStatus;
import com.im.server.comparison.api.ComparisonRunView;
import com.im.server.condition.api.ConditionApi;
import com.im.server.decision.api.DecisionType;
import com.im.server.decision.domain.Decision;
import com.im.server.decision.domain.SignatureSession;
import com.im.server.decision.internal.DecisionRepository;
import com.im.server.decision.internal.ReviewGateEvaluator;
import com.im.server.decision.internal.SignatureSessionRepository;
import com.im.server.shared.event.DecisionSubmittedEvent;
import com.im.server.shared.exception.BusinessException;
import com.im.server.shared.exception.ErrorCode;
import com.im.server.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
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
  private final SignatureSessionRepository signatureSessionRepository;
  private final ReviewGateEvaluator reviewGateEvaluator;
  private final ApplicationEventPublisher eventPublisher;

  public SubmitDecisionHandler(
      ComparisonApi comparisonApi,
      ConditionApi conditionApi,
      DecisionRepository decisionRepository,
      SignatureSessionRepository signatureSessionRepository,
      ReviewGateEvaluator reviewGateEvaluator,
      ApplicationEventPublisher eventPublisher) {
    this.comparisonApi = comparisonApi;
    this.conditionApi = conditionApi;
    this.decisionRepository = decisionRepository;
    this.signatureSessionRepository = signatureSessionRepository;
    this.reviewGateEvaluator = reviewGateEvaluator;
    this.eventPublisher = eventPublisher;
  }

  @Operation(summary = "약정 결정 저장", description = "PROCEED는 필수 확인이 모두 끝나고 유효한 서명 세션이 있어야 저장할 수 있다.")
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

    boolean allReviewed = reviewGateEvaluator.isAllReviewed(comparisonId);

    if (command.decisionType() == DecisionType.PROCEED) {
      if (!allReviewed) {
        throw new BusinessException(ErrorCode.DECISION_REVIEW_GATE_NOT_CLEARED);
      }
      validateSignatureSession(comparisonId, command.signatureSessionId());
      // FR03 — 서명 세션 발급 이후 최종 약정서 Version이 또 바뀌었으면(재심사 등) 재비교부터 다시 해야 한다.
      if (!conditionApi
          .getLatestPostSnapshot(run.applicationId())
          .id()
          .equals(run.postSnapshotId())) {
        throw new BusinessException(ErrorCode.DECISION_CONTRACT_VERSION_CHANGED);
      }
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

    if (decision.getDecisionType() == DecisionType.PROCEED) {
      eventPublisher.publishEvent(new DecisionSubmittedEvent(decision.getId(), comparisonId));
    }

    return ResponseEntity.ok(
        ApiResponse.success(
            new SubmitDecisionResponse(
                decision.getId(), decision.getDecisionType(), decision.getSignedAt())));
  }

  private void validateSignatureSession(Long comparisonId, String signatureSessionId) {
    if (signatureSessionId == null || signatureSessionId.isBlank()) {
      throw new BusinessException(ErrorCode.DECISION_SIGNATURE_SESSION_INVALID);
    }
    SignatureSession session =
        signatureSessionRepository
            .findBySessionId(signatureSessionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DECISION_SIGNATURE_SESSION_INVALID));
    if (!session.isUsable(comparisonId, Instant.now())) {
      throw new BusinessException(ErrorCode.DECISION_SIGNATURE_SESSION_INVALID);
    }
    session.markUsed();
    signatureSessionRepository.save(session);
  }
}
