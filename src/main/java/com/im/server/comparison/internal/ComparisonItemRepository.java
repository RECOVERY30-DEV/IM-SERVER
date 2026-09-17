package com.im.server.comparison.internal;

import com.im.server.comparison.domain.ComparisonItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComparisonItemRepository extends JpaRepository<ComparisonItem, Long> {

  List<ComparisonItem> findByComparisonRunId(Long comparisonRunId);

  List<ComparisonItem> findByComparisonRunIdAndRequiresReviewTrue(Long comparisonRunId);
}
