package com.im.server.comparison.internal;

import com.im.server.comparison.domain.ComparisonRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComparisonRunRepository extends JpaRepository<ComparisonRun, Long> {

  boolean existsByPreSnapshotIdAndPostSnapshotIdAndRuleVersionAndCalculationVersion(
      Long preSnapshotId, Long postSnapshotId, String ruleVersion, String calculationVersion);
}
