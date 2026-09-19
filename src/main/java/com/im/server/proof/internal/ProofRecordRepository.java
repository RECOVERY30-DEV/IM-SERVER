package com.im.server.proof.internal;

import com.im.server.proof.domain.ProofRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProofRecordRepository extends JpaRepository<ProofRecord, Long> {

  Optional<ProofRecord> findByDecisionId(Long decisionId);

  Optional<ProofRecord> findByProofId(String proofId);
}
