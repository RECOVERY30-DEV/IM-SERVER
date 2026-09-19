package com.im.server.decision.internal;

import com.im.server.comparison.api.ComparisonApi;
import com.im.server.comparison.api.ComparisonItemView;
import com.im.server.decision.domain.DecisionRequiredReview;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** FR11 Review Gate 판정 — 서명 세션 발급과 Decision 저장이 같은 기준을 쓴다. */
@Component
public class ReviewGateEvaluator {

  private final ComparisonApi comparisonApi;
  private final DecisionRequiredReviewRepository decisionRequiredReviewRepository;

  public ReviewGateEvaluator(
      ComparisonApi comparisonApi,
      DecisionRequiredReviewRepository decisionRequiredReviewRepository) {
    this.comparisonApi = comparisonApi;
    this.decisionRequiredReviewRepository = decisionRequiredReviewRepository;
  }

  public boolean isAllReviewed(Long comparisonRunId) {
    List<ComparisonItemView> requiredItems = comparisonApi.listRequiredReviewItems(comparisonRunId);
    if (requiredItems.isEmpty()) {
      return true;
    }
    Set<Long> reviewedItemIds =
        decisionRequiredReviewRepository.findByComparisonRunId(comparisonRunId).stream()
            .filter(DecisionRequiredReview::isReviewed)
            .map(DecisionRequiredReview::getComparisonItemId)
            .collect(Collectors.toSet());
    return requiredItems.stream().map(ComparisonItemView::id).allMatch(reviewedItemIds::contains);
  }
}
