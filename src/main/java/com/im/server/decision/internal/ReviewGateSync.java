package com.im.server.decision.internal;

import com.im.server.comparison.api.ComparisonItemView;
import com.im.server.decision.domain.DecisionRequiredReview;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * comparison_items.requires_review인 항목마다 decision_required_reviews 행이 있어야 한다. comparison 모듈은 이 테이블에
 * 쓰지 않으므로(모듈 경계), 처음 조회하는 시점에 decision 모듈이 없는 행을 채워 넣는다.
 */
@Component
public class ReviewGateSync {

  private final DecisionRequiredReviewRepository decisionRequiredReviewRepository;

  public ReviewGateSync(DecisionRequiredReviewRepository decisionRequiredReviewRepository) {
    this.decisionRequiredReviewRepository = decisionRequiredReviewRepository;
  }

  public List<DecisionRequiredReview> ensure(
      Long comparisonRunId, List<ComparisonItemView> requiredItems) {
    return requiredItems.stream()
        .map(
            item ->
                decisionRequiredReviewRepository
                    .findByComparisonItemId(item.id())
                    .orElseGet(
                        () ->
                            decisionRequiredReviewRepository.save(
                                new DecisionRequiredReview(comparisonRunId, item.id()))))
        .toList();
  }
}
