package com.im.server.decision.internal;

import com.im.server.decision.domain.Decision;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionRepository extends JpaRepository<Decision, Long> {

  Optional<Decision> findByComparisonRunId(Long comparisonRunId);
}
