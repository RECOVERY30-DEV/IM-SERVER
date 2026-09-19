package com.im.server.decision.reviewcomparisonitem;

import com.im.server.comparison.api.ComparisonApi;
import com.im.server.comparison.api.ComparisonItemView;
import com.im.server.decision.domain.DecisionRequiredReview;
import com.im.server.decision.internal.DecisionRequiredReviewRepository;
import com.im.server.shared.exception.BusinessException;
import com.im.server.shared.exception.ErrorCode;
import com.im.server.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** FR11 — S04 "확인" 버튼. 이미 확인된 항목을 다시 호출해도 idempotent하게 200을 반환한다. */
@RestController
@Tag(name = "decision", description = "Review Gate / 약정 결정 / 전자서명")
public class ReviewComparisonItemHandler {

  private final ComparisonApi comparisonApi;
  private final DecisionRequiredReviewRepository decisionRequiredReviewRepository;

  public ReviewComparisonItemHandler(
      ComparisonApi comparisonApi,
      DecisionRequiredReviewRepository decisionRequiredReviewRepository) {
    this.comparisonApi = comparisonApi;
    this.decisionRequiredReviewRepository = decisionRequiredReviewRepository;
  }

  @Operation(
      summary = "비교 항목 확인 처리",
      description = "확인이 필요한(WORSE/STRUCTURAL_CHANGE/UNKNOWN) 항목만 처리할 수 있다.")
  @PostMapping("/api/comparisons/items/{itemId}:review")
  public ResponseEntity<ApiResponse<ReviewItemResponse>> handle(@PathVariable Long itemId) {
    ComparisonItemView item = comparisonApi.getItem(itemId);
    if (!item.requiresReview()) {
      throw new BusinessException(ErrorCode.COMPARISON_ITEM_REVIEW_NOT_REQUIRED);
    }

    DecisionRequiredReview review =
        decisionRequiredReviewRepository
            .findByComparisonItemId(itemId)
            .orElseGet(() -> new DecisionRequiredReview(item.comparisonRunId(), itemId));
    review.markReviewed();
    decisionRequiredReviewRepository.save(review);

    return ResponseEntity.ok(
        ApiResponse.success(new ReviewItemResponse(itemId, review.getReviewedAt())));
  }
}
