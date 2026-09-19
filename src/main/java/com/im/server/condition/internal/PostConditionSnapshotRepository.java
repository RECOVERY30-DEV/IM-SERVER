package com.im.server.condition.internal;

import com.im.server.condition.domain.PostConditionSnapshot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostConditionSnapshotRepository
    extends JpaRepository<PostConditionSnapshot, Long> {

  Optional<PostConditionSnapshot> findByContractDocumentIdAndContractDocumentHash(
      String contractDocumentId, String contractDocumentHash);
}
