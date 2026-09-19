package com.im.server.decision.internal;

import com.im.server.decision.domain.DecisionRequiredReview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionRequiredReviewRepository
    extends JpaRepository<DecisionRequiredReview, Long> {

  Optional<DecisionRequiredReview> findByComparisonItemId(Long comparisonItemId);

  List<DecisionRequiredReview> findByComparisonRunId(Long comparisonRunId);
}
