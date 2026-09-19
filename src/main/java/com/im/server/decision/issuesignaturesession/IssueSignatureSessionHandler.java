package com.im.server.decision.issuesignaturesession;

import com.im.server.comparison.api.ComparisonApi;
import com.im.server.comparison.api.ComparisonRunStatus;
import com.im.server.comparison.api.ComparisonRunView;
import com.im.server.decision.domain.SignatureSession;
import com.im.server.decision.internal.ReviewGateEvaluator;
import com.im.server.decision.internal.SignatureSessionRepository;
import com.im.server.shared.exception.BusinessException;
import com.im.server.shared.exception.ErrorCode;
import com.im.server.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** S05 "전자서명" 패드 진입 시 발급하는 서명 유효시간 세션. */
@RestController
@Tag(name = "decision", description = "Review Gate / 약정 결정 / 전자서명")
public class IssueSignatureSessionHandler {

  private static final Duration SESSION_TTL = Duration.ofMinutes(5);

  private final ComparisonApi comparisonApi;
  private final ReviewGateEvaluator reviewGateEvaluator;
  private final SignatureSessionRepository signatureSessionRepository;

  public IssueSignatureSessionHandler(
      ComparisonApi comparisonApi,
      ReviewGateEvaluator reviewGateEvaluator,
      SignatureSessionRepository signatureSessionRepository) {
    this.comparisonApi = comparisonApi;
    this.reviewGateEvaluator = reviewGateEvaluator;
    this.signatureSessionRepository = signatureSessionRepository;
  }

  @Operation(summary = "서명 세션 발급", description = "필수 확인이 끝나야 발급된다. 유효시간(5분) 내에만 결정 저장에 쓸 수 있다.")
  @PostMapping("/api/comparisons/{comparisonId}/signature-sessions")
  public ResponseEntity<ApiResponse<IssueSignatureSessionResponse>> handle(
      @PathVariable Long comparisonId) {
    ComparisonRunView run = comparisonApi.getRun(comparisonId);
    if (run.status() != ComparisonRunStatus.COMPLETED) {
      throw new BusinessException(ErrorCode.COMPARISON_RUN_NOT_COMPLETED);
    }
    if (!reviewGateEvaluator.isAllReviewed(comparisonId)) {
      throw new BusinessException(ErrorCode.DECISION_REVIEW_GATE_NOT_CLEARED);
    }

    Instant validUntil = Instant.now().plus(SESSION_TTL);
    SignatureSession session =
        new SignatureSession(comparisonId, UUID.randomUUID().toString(), validUntil);
    signatureSessionRepository.save(session);

    return ResponseEntity.ok(
        ApiResponse.success(
            new IssueSignatureSessionResponse(session.getSessionId(), session.getValidUntil())));
  }
}
