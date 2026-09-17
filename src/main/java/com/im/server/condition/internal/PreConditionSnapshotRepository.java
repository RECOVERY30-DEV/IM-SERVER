package com.im.server.condition.internal;

import com.im.server.condition.domain.PreConditionSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreConditionSnapshotRepository extends JpaRepository<PreConditionSnapshot, Long> {

  Optional<PreConditionSnapshot> findByIdempotencyKey(String idempotencyKey);

  Optional<PreConditionSnapshot> findFirstByApplicationIdOrderByCreatedAtDesc(String applicationId);

  List<PreConditionSnapshot> findByApplicationIdOrderByCreatedAtDesc(String applicationId);
}
