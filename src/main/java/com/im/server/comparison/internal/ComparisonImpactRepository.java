package com.im.server.comparison.internal;

import com.im.server.comparison.domain.ComparisonImpact;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComparisonImpactRepository extends JpaRepository<ComparisonImpact, Long> {

  Optional<ComparisonImpact> findByComparisonRunId(Long comparisonRunId);
}
