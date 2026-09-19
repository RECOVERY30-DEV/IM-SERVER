package com.im.server.proof.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockLedgerRepository extends JpaRepository<MockLedgerEntry, Long> {

  Optional<MockLedgerEntry> findByRecordIdHash(String recordIdHash);

  Optional<MockLedgerEntry> findByLedgerReference(String ledgerReference);
}
